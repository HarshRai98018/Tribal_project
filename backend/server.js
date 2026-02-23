import cors from "cors";
import express from "express";
import mysql from "mysql2/promise";
import crypto from "crypto";

const app = express();
app.use(cors());
app.use(express.json());

const DB_CONFIG = {
  host: process.env.DB_HOST || "localhost",
  port: Number(process.env.DB_PORT || 3306),
  user: process.env.DB_USER || "root",
  password: process.env.DB_PASSWORD || "Harsh@8520",
  database: process.env.DB_NAME || "tribal_database",
  waitForConnections: true,
  connectionLimit: 10
};

const ALLOWED_ROLES = new Set(["ADMIN", "ARTISAN", "CUSTOMER", "CONSULTANT"]);
const SELF_REGISTER_ROLES = new Set(["ARTISAN", "CUSTOMER", "CONSULTANT"]);
const PASSWORD_HASH_ITERATIONS = 100000;
const PASSWORD_HASH_BYTES = 64;
const PASSWORD_HASH_DIGEST = "sha512";

const pool = mysql.createPool(DB_CONFIG);
let usersSchema = null;

const normalizeRole = (role) => String(role || "CUSTOMER").toUpperCase().trim();

const sanitizeUser = (row) => ({
  id: row.id,
  email: row.email,
  role: normalizeRole(row.role)
});

const parseUserId = (value) => {
  const parsedId = Number(value);
  return Number.isInteger(parsedId) && parsedId > 0 ? parsedId : null;
};

const wrapIdentifier = (value) => `\`${String(value).replace(/`/g, "")}\``;

const isHashedPassword = (value) =>
  /^[a-f0-9]{32}:[a-f0-9]{128}$/i.test(String(value || ""));

const hashPassword = (password) => {
  const salt = crypto.randomBytes(16).toString("hex");
  const hash = crypto
    .pbkdf2Sync(
      String(password),
      salt,
      PASSWORD_HASH_ITERATIONS,
      PASSWORD_HASH_BYTES,
      PASSWORD_HASH_DIGEST
    )
    .toString("hex");

  return `${salt}:${hash}`;
};

const verifyPassword = (inputPassword, storedPassword) => {
  const rawInput = String(inputPassword || "");
  const rawStored = String(storedPassword || "");

  if (!rawStored) {
    return { ok: false, shouldUpgrade: false };
  }

  if (!isHashedPassword(rawStored)) {
    return { ok: rawInput === rawStored, shouldUpgrade: rawInput === rawStored };
  }

  const [salt, expectedHash] = rawStored.split(":");
  const derivedHash = crypto
    .pbkdf2Sync(
      rawInput,
      salt,
      PASSWORD_HASH_ITERATIONS,
      PASSWORD_HASH_BYTES,
      PASSWORD_HASH_DIGEST
    )
    .toString("hex");

  const isMatch =
    expectedHash.length === derivedHash.length &&
    crypto.timingSafeEqual(Buffer.from(expectedHash, "hex"), Buffer.from(derivedHash, "hex"));

  return { ok: isMatch, shouldUpgrade: false };
};

const getUsersSchema = async () => {
  if (usersSchema) {
    return usersSchema;
  }

  const [columns] = await pool.query("SHOW COLUMNS FROM users");
  const columnSet = new Set(columns.map((column) => column.Field));

  const detected = {
    id: columnSet.has("user_id")
      ? "user_id"
      : columnSet.has("id")
        ? "id"
        : null,
    email: columnSet.has("email") ? "email" : null,
    password: columnSet.has("password_hash")
      ? "password_hash"
      : columnSet.has("password")
        ? "password"
        : null,
    role: columnSet.has("role") ? "role" : null,
    createdAt: columnSet.has("created_at") ? "created_at" : null
  };

  if (!detected.id || !detected.email || !detected.password || !detected.role) {
    throw new Error("Users table has unsupported schema.");
  }

  usersSchema = detected;
  return usersSchema;
};

const ensureUsersTable = async () => {
  await pool.query(`
    CREATE TABLE IF NOT EXISTS users (
      user_id INT PRIMARY KEY AUTO_INCREMENT,
      email VARCHAR(255) NOT NULL UNIQUE,
      password_hash VARCHAR(255) NOT NULL,
      role ENUM('ADMIN', 'ARTISAN', 'CUSTOMER', 'CONSULTANT') NOT NULL DEFAULT 'CUSTOMER',
      created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
    )
  `);

  await getUsersSchema();
};

const initDatabase = async () => {
  await ensureUsersTable();
  console.log("Auth table ready");
};

app.get("/health", (_req, res) => {
  res.json({ ok: true });
});

app.post("/auth/register", async (req, res) => {
  try {
    await ensureUsersTable();
    const schema = await getUsersSchema();

    const email = String(req.body?.email || "").trim().toLowerCase();
    const password = String(req.body?.password || "");
    const role = normalizeRole(req.body?.role);

    if (!email || !password) {
      return res.status(400).json({ message: "Email and password are required." });
    }

    if (password.length < 6) {
      return res.status(400).json({ message: "Password must be at least 6 characters." });
    }

    if (!ALLOWED_ROLES.has(role)) {
      return res.status(400).json({ message: "Invalid role selected." });
    }

    if (!SELF_REGISTER_ROLES.has(role)) {
      return res.status(403).json({ message: "This role cannot be self-registered." });
    }

    const [existing] = await pool.query(
      `SELECT ${wrapIdentifier(schema.id)} AS id FROM users WHERE ${wrapIdentifier(schema.email)} = ? LIMIT 1`,
      [email]
    );

    if (existing.length > 0) {
      return res.status(409).json({ message: "Email already registered." });
    }

    const [insertResult] = await pool.query(
      `INSERT INTO users (${wrapIdentifier(schema.email)}, ${wrapIdentifier(schema.password)}, ${wrapIdentifier(schema.role)}) VALUES (?, ?, ?)`,
      [email, hashPassword(password), role]
    );

    const user = {
      id: insertResult.insertId,
      email,
      role
    };

    return res.status(201).json({ message: "Account created.", user });
  } catch (error) {
    console.error("Register error:", error);
    return res.status(500).json({ message: "Unable to create account right now." });
  }
});

app.post("/auth/login", async (req, res) => {
  try {
    await ensureUsersTable();
    const schema = await getUsersSchema();

    const email = String(req.body?.email || "").trim().toLowerCase();
    const password = String(req.body?.password || "");

    if (!email || !password) {
      return res.status(400).json({ message: "Email and password are required." });
    }

    const [rows] = await pool.query(
      `SELECT
        ${wrapIdentifier(schema.id)} AS id,
        ${wrapIdentifier(schema.email)} AS email,
        ${wrapIdentifier(schema.password)} AS password_value,
        ${wrapIdentifier(schema.role)} AS role
      FROM users
      WHERE ${wrapIdentifier(schema.email)} = ?
      LIMIT 1`,
      [email]
    );

    if (rows.length === 0) {
      return res.status(401).json({ message: "Invalid email or password." });
    }

    const passwordCheck = verifyPassword(password, rows[0].password_value);
    if (!passwordCheck.ok) {
      return res.status(401).json({ message: "Invalid email or password." });
    }

    if (passwordCheck.shouldUpgrade) {
      await pool.query(
        `UPDATE users SET ${wrapIdentifier(schema.password)} = ? WHERE ${wrapIdentifier(schema.id)} = ?`,
        [hashPassword(password), rows[0].id]
      );
    }

    const normalizedRole = normalizeRole(rows[0].role);
    if (!ALLOWED_ROLES.has(normalizedRole)) {
      return res.status(403).json({ message: "Role is not allowed in this app." });
    }

    return res.json({
      message: "Login successful.",
      user: sanitizeUser({ ...rows[0], role: normalizedRole })
    });
  } catch (error) {
    console.error("Login error:", error);
    return res.status(500).json({ message: "Unable to login right now." });
  }
});

app.get("/admin/users", async (_req, res) => {
  try {
    await ensureUsersTable();
    const schema = await getUsersSchema();

    const createdAtSelection = schema.createdAt
      ? `${wrapIdentifier(schema.createdAt)} AS createdAt`
      : "NULL AS createdAt";

    const [rows] = await pool.query(`
      SELECT
        ${wrapIdentifier(schema.id)} AS id,
        ${wrapIdentifier(schema.email)} AS email,
        ${wrapIdentifier(schema.role)} AS role,
        ${createdAtSelection}
      FROM users
      ORDER BY ${wrapIdentifier(schema.id)} DESC
    `);

    return res.json({
      users: rows.map((row) => ({
        id: row.id,
        email: row.email,
        role: normalizeRole(row.role),
        createdAt: row.createdAt
      }))
    });
  } catch (error) {
    console.error("List users error:", error);
    return res.status(500).json({ message: "Unable to load users right now." });
  }
});

app.put("/admin/users/:id", async (req, res) => {
  try {
    await ensureUsersTable();
    const schema = await getUsersSchema();

    const userId = parseUserId(req.params.id);
    if (!userId) {
      return res.status(400).json({ message: "Invalid user id." });
    }

    const [rows] = await pool.query(
      `SELECT
        ${wrapIdentifier(schema.id)} AS id,
        ${wrapIdentifier(schema.email)} AS email,
        ${wrapIdentifier(schema.role)} AS role,
        ${wrapIdentifier(schema.password)} AS password_value
      FROM users
      WHERE ${wrapIdentifier(schema.id)} = ?
      LIMIT 1`,
      [userId]
    );

    if (rows.length === 0) {
      return res.status(404).json({ message: "User not found." });
    }

    const currentUser = rows[0];
    const nextEmail =
      req.body?.email !== undefined
        ? String(req.body.email).trim().toLowerCase()
        : currentUser.email;
    const nextRole =
      req.body?.role !== undefined ? normalizeRole(req.body.role) : currentUser.role;
    const nextPassword =
      req.body?.password !== undefined && String(req.body.password).length > 0
        ? hashPassword(String(req.body.password))
        : currentUser.password_value;

    if (!nextEmail) {
      return res.status(400).json({ message: "Email is required." });
    }

    if (!ALLOWED_ROLES.has(nextRole)) {
      return res.status(400).json({ message: "Invalid role selected." });
    }

    if (!nextPassword || nextPassword.length < 6) {
      return res.status(400).json({ message: "Password must be at least 6 characters." });
    }

    const [existing] = await pool.query(
      `SELECT ${wrapIdentifier(schema.id)} AS id
      FROM users
      WHERE ${wrapIdentifier(schema.email)} = ? AND ${wrapIdentifier(schema.id)} <> ?
      LIMIT 1`,
      [nextEmail, userId]
    );

    if (existing.length > 0) {
      return res.status(409).json({ message: "Another user already has this email." });
    }

    await pool.query(
      `UPDATE users
      SET ${wrapIdentifier(schema.email)} = ?, ${wrapIdentifier(schema.role)} = ?, ${wrapIdentifier(schema.password)} = ?
      WHERE ${wrapIdentifier(schema.id)} = ?`,
      [nextEmail, nextRole, nextPassword, userId]
    );

    return res.json({
      message: "User updated.",
      user: {
        id: userId,
        email: nextEmail,
        role: nextRole
      }
    });
  } catch (error) {
    console.error("Update user error:", error);
    return res.status(500).json({ message: "Unable to update user right now." });
  }
});

app.delete("/admin/users/:id", async (req, res) => {
  try {
    await ensureUsersTable();
    const schema = await getUsersSchema();

    const userId = parseUserId(req.params.id);
    if (!userId) {
      return res.status(400).json({ message: "Invalid user id." });
    }

    const [result] = await pool.query(
      `DELETE FROM users WHERE ${wrapIdentifier(schema.id)} = ?`,
      [userId]
    );

    if (result.affectedRows === 0) {
      return res.status(404).json({ message: "User not found." });
    }

    return res.json({ message: "User deleted." });
  } catch (error) {
    console.error("Delete user error:", error);
    return res.status(500).json({ message: "Unable to delete user right now." });
  }
});

const port = Number(process.env.PORT || 5000);

const start = async () => {
  try {
    await initDatabase();
    app.listen(port, () => {
      console.log(`Backend running on port ${port}`);
    });
  } catch (error) {
    console.error("Startup error:", error);
    process.exit(1);
  }
};

start();
