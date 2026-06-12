"""
SQL Deep Dive Tutorial - 01: SQL Fundamentals
Covers: Relational DB, DDL, DML, SELECT, Aggregates, JOINs,
        Subqueries, NULL, Constraints, Views
"""
import sys, math, os
sys.path.insert(0, '/sessions/awesome-sleepy-hawking/mnt/outputs')
from pdf_utils import *

OUT = "/sessions/awesome-sleepy-hawking/mnt/Final/SQL_Deep_Dive_Tutorial/01_SQL_Fundamentals.pdf"
os.makedirs(os.path.dirname(OUT), exist_ok=True)

doc = make_doc(OUT, "SQL Fundamentals — Deep Dive Tutorial", "SQL Deep Dive Series")

# ── colour aliases ────────────────────────────────────────────────────────
TEAL  = cd("#00695C")
PURP  = cd("#4527A0")
ORG   = cd("#E65100")
BLUE  = cd("#0D47A1")
LBLUE = cd("#E3F2FD")
LGRN  = cd("#E8F5E9")
LPUR  = cd("#EDE7F6")
LORG  = cd("#FFF3E0")
DGRY  = cd("#37474F")
MGRY  = cd("#78909C")

# ─────────────────────────────────────────────────────────────────────────
# SECTION HEADER COLOURS
# A=BG  B=TEAL  C=ORG  D=BLUE  E=PURP  F=RED  G=GRN  H=DGRY  I=MID  J=TEAL
SEC_BG = {
    'A': BG, 'B': TEAL, 'C': ORG, 'D': BLUE,
    'E': PURP, 'F': cd("#B71C1C"), 'G': GRN,
    'H': DGRY, 'I': MID, 'J': TEAL
}

def sec(letter, title, subtitle):
    return hdr(letter, title, subtitle, SEC_BG.get(letter, BG))

def tip(text):
    t = Table([[Paragraph(f"<b>TIP:</b> {text}", St("Normal", fontSize=9,
               textColor=cd("#1B5E20"), leading=13))]],
              colWidths=[16*cm])
    t.setStyle(TableStyle([("BACKGROUND",(0,0),(-1,-1),cd("#F1F8E9")),
        ("BOX",(0,0),(-1,-1),0.8,GRN),
        ("LEFTPADDING",(0,0),(-1,-1),8),("TOPPADDING",(0,0),(-1,-1),6),
        ("BOTTOMPADDING",(0,0),(-1,-1),6)]))
    return t

def warn(text):
    t = Table([[Paragraph(f"<b>WARNING:</b> {text}", St("Normal", fontSize=9,
               textColor=cd("#B71C1C"), leading=13))]],
              colWidths=[16*cm])
    t.setStyle(TableStyle([("BACKGROUND",(0,0),(-1,-1),cd("#FFEBEE")),
        ("BOX",(0,0),(-1,-1),0.8,RED),
        ("LEFTPADDING",(0,0),(-1,-1),8),("TOPPADDING",(0,0),(-1,-1),6),
        ("BOTTOMPADDING",(0,0),(-1,-1),6)]))
    return t

def info_box(label, text, bg=LBLUE, border=BLUE):
    t = Table([[Paragraph(f"<b>{label}:</b> {text}", St("Normal", fontSize=9,
               textColor=DGRY, leading=13))]],
              colWidths=[16*cm])
    t.setStyle(TableStyle([("BACKGROUND",(0,0),(-1,-1),bg),
        ("BOX",(0,0),(-1,-1),0.8,border),
        ("LEFTPADDING",(0,0),(-1,-1),8),("TOPPADDING",(0,0),(-1,-1),6),
        ("BOTTOMPADDING",(0,0),(-1,-1),6)]))
    return t

def step_label(n, title):
    return Paragraph(f"<b><font color='#0D47A1'>Step {n} — {title}</font></b>", H2)

def bullet(text):
    return Paragraph(f"• {text}", BL)

# ─────────────────────────────────────────────────────────────────────────
story = []

# ══════════════════════════════════════════════════════════════════════════
# COVER
# ══════════════════════════════════════════════════════════════════════════
story += cover_table(
    "SQL Fundamentals",
    "Deep Dive Tutorial — Chapter 01",
    "Relational Databases | DDL | DML | SELECT | JOINs | NULLs | Views",
    [
        (BG,   "A. Relational Databases",     "Tables, rows, columns, relationships"),
        (TEAL, "B. DDL",                       "CREATE, ALTER, DROP with constraints"),
        (ORG,  "C. DML",                       "INSERT, UPDATE, DELETE"),
        (BLUE, "D. SELECT Basics",             "WHERE, ORDER BY, LIMIT"),
        (PURP, "E. Aggregate Functions",       "COUNT, SUM, AVG, GROUP BY, HAVING"),
        (RED,  "F. JOINs",                     "INNER, LEFT, RIGHT, FULL OUTER"),
        (GRN,  "G. Subqueries",                "Correlated, non-correlated, EXISTS vs IN"),
        (DGRY, "H. NULL Handling",             "IS NULL, COALESCE, three-valued logic"),
        (MID,  "I. Constraints & Referential Integrity", "CASCADE DELETE/UPDATE"),
        (TEAL, "J. Views",                     "Updatable vs non-updatable views"),
    ]
)
story.append(PageBreak())

# ══════════════════════════════════════════════════════════════════════════
# A. WHAT IS A RELATIONAL DATABASE?
# ══════════════════════════════════════════════════════════════════════════
story.append(sec('A', "What is a Relational Database?", "Tables · Rows · Columns · Relationships"))
story.append(sp(8))

story.append(step_label(1, "Plain English Explanation"))
story.append(Paragraph(
    "A relational database organises data into <b>tables</b> (also called relations). Each table "
    "represents one real-world entity type — customers, orders, products. A table has "
    "<b>columns</b> (attributes, e.g. customer_id, name, email) and <b>rows</b> (records, one "
    "per individual entity instance). Tables are linked by <b>relationships</b> — a foreign key "
    "in one table references the primary key of another, so you can answer questions that span "
    "multiple entities without duplicating data. The whole model is built on <b>set theory</b> "
    "and <b>predicate logic</b> — every query is a mathematical operation on sets of rows.", NR))
story.append(sp(4))

story.append(step_label(2, "Real-World Analogy"))
story.append(Paragraph(
    "Think of a relational database as a well-organised office filing system. Each <b>cabinet "
    "drawer</b> is a table. Each <b>folder inside</b> is a row (one customer, one order). Each "
    "<b>labelled field on the form</b> inside the folder is a column. A sticky note that says "
    "'see drawer ORDERS, folder #1042' is a foreign key — it points you to related data in "
    "another drawer without copying all that data again.", NR))
story.append(sp(4))

story.append(step_label(3, "Minimal SQL Example"))
story.append(cb("""
    -- Two related tables: customers and orders
    CREATE TABLE customers (
        customer_id  INT         PRIMARY KEY,
        name         VARCHAR(80) NOT NULL,
        email        VARCHAR(120) UNIQUE
    );

    CREATE TABLE orders (
        order_id     INT  PRIMARY KEY,
        customer_id  INT  NOT NULL,          -- foreign key column
        amount       DECIMAL(10,2),
        FOREIGN KEY (customer_id) REFERENCES customers(customer_id)
    );
"""))
story.append(sp(4))

story.append(step_label(4, "Diagram — Relational Model"))

def diag_relational(c):
    W, H = 460, 170
    diag_title(c, W, H, "Relational Database: Tables and Relationships")
    # customers table
    ubox(c, 20, 60, 175, 100, "customers", bg=LBLUE, border=BLUE)
    note(c, 107, 145, "PK: customer_id", 7, BLUE)
    note(c, 107, 133, "name   VARCHAR(80)", 7, MGRY)
    note(c, 107, 121, "email  VARCHAR(120)", 7, MGRY)
    note(c, 107, 109, "< 1 row per customer >", 7, MGRY)
    # orders table
    ubox(c, 270, 60, 175, 100, "orders", bg=LORG, border=ORG)
    note(c, 357, 145, "PK: order_id", 7, ORG)
    note(c, 357, 133, "FK: customer_id", 7, cd("#B71C1C"))
    note(c, 357, 121, "amount DECIMAL", 7, MGRY)
    note(c, 357, 109, "< 1 row per order >", 7, MGRY)
    # relationship arrow
    arr(c, 195, 110, 270, 110, lbl="1 customer : N orders", ly=8)
    note(c, 235, 95, "FK references PK", 7, RED)
    # labels
    note(c, 107, 70, "TABLE", 6.5, BLUE)
    note(c, 357, 70, "TABLE", 6.5, ORG)

story += diag_wrap(Diag(diag_relational, 460, 170),
                   "Fig A.1 — customers and orders tables linked by foreign key")
story.append(sp(4))

story.append(step_label(5, "Behind the Scenes — What the DB Engine Does"))
story.append(Paragraph(
    "When you create a table the engine allocates <b>pages</b> (typically 8 KB or 16 KB blocks) "
    "in a data file. Row data is stored inside those pages. The engine maintains a <b>system "
    "catalog</b> (metadata tables) that records every table name, column name, data type, "
    "constraint, and index. When you insert a row the engine finds a page with enough free "
    "space, writes the row, and updates the catalog statistics. The <b>buffer pool</b> keeps "
    "recently used pages in RAM to avoid expensive disk reads.", NR))
story.append(sp(4))

story.append(step_label(6, "Realistic Practical Example"))
story.append(Paragraph(
    "An e-commerce platform has: <b>customers</b>, <b>products</b>, <b>orders</b>, and "
    "<b>order_items</b> (a junction/bridge table). Instead of repeating the customer name "
    "in every order row, you store customer_id. This eliminates duplication, saves disk space, "
    "and means a name change only requires updating one row in customers — not thousands of "
    "order rows. This is called <b>normalisation</b>.", NR))
story.append(sp(4))

story.append(step_label(7, "Common Mistakes and Misconceptions"))
story += [
    bullet("Thinking a 'database' and a 'table' are the same thing. A database contains many tables."),
    bullet("Confusing rows with columns. Columns define structure; rows hold data."),
    bullet("Storing repeated data (e.g. customer name in every order row). Use a foreign key instead."),
    bullet("Assuming row order is guaranteed. SQL tables are unordered sets — always use ORDER BY for sorted results."),
    bullet("Not defining a PRIMARY KEY. Without one, the engine cannot uniquely identify a row."),
]
story.append(sp(6))
story.append(hr())

# ══════════════════════════════════════════════════════════════════════════
# B. DDL
# ══════════════════════════════════════════════════════════════════════════
story.append(PageBreak())
story.append(sec('B', "DDL — Data Definition Language", "CREATE TABLE · ALTER TABLE · DROP TABLE · Constraints"))
story.append(sp(8))

story.append(step_label(1, "Plain English Explanation"))
story.append(Paragraph(
    "DDL (Data Definition Language) is the subset of SQL used to define and modify the "
    "<b>structure</b> of the database — tables, columns, constraints, and indexes. Unlike DML "
    "(which changes data), DDL changes the schema itself. The three core DDL statements are "
    "<b>CREATE TABLE</b> (build a new table), <b>ALTER TABLE</b> (modify an existing table), "
    "and <b>DROP TABLE</b> (permanently remove a table and all its data). Constraints — "
    "PRIMARY KEY, FOREIGN KEY, NOT NULL, UNIQUE, CHECK — are also declared in DDL and are "
    "enforced by the engine on every DML operation.", NR))
story.append(sp(4))

story.append(step_label(2, "Real-World Analogy"))
story.append(Paragraph(
    "DDL is like designing and printing the blank form used in an office. CREATE TABLE prints "
    "the form (defines which fields exist). ALTER TABLE is like adding a new field to an "
    "existing form. DROP TABLE is shredding all copies. Constraints are the validation rules "
    "printed on the form: 'Employee ID is required', 'Email must be unique', 'Department must "
    "exist in the Departments list'.", NR))
story.append(sp(4))

story.append(step_label(3, "Minimal SQL Example"))
story.append(cb("""
    -- CREATE TABLE with all major constraint types
    CREATE TABLE employees (
        emp_id      INT           PRIMARY KEY,         -- unique, not null
        dept_id     INT           NOT NULL,
        name        VARCHAR(100)  NOT NULL,
        email       VARCHAR(150)  UNIQUE,               -- no duplicates
        salary      DECIMAL(10,2) CHECK (salary > 0),  -- value rule
        hire_date   DATE          DEFAULT CURRENT_DATE,
        FOREIGN KEY (dept_id) REFERENCES departments(dept_id)
    );

    -- ALTER TABLE: add a column
    ALTER TABLE employees ADD COLUMN phone VARCHAR(20);

    -- DROP TABLE (permanent!)
    DROP TABLE employees;
"""))
story.append(sp(4))

story.append(step_label(4, "Diagram — DDL Constraint Map"))

def diag_ddl(c):
    W, H = 460, 200
    diag_title(c, W, H, "DDL: Constraints on the employees Table")
    # central table
    ubox(c, 155, 60, 150, 120, "employees", bg=LBLUE, border=BLUE)
    note(c, 230, 165, "emp_id  PK", 7, cd("#B71C1C"))
    note(c, 230, 153, "dept_id NOT NULL FK", 7, MGRY)
    note(c, 230, 141, "name    NOT NULL", 7, MGRY)
    note(c, 230, 129, "email   UNIQUE", 7, MGRY)
    note(c, 230, 117, "salary  CHECK > 0", 7, MGRY)
    note(c, 230, 105, "hire_date DEFAULT", 7, MGRY)
    # departments box
    ubox(c, 330, 90, 110, 60, "departments", bg=LGRN, border=GRN)
    note(c, 385, 138, "dept_id PK", 7, GRN)
    arr(c, 305, 130, 330, 130, lbl="FK ref", ly=8)
    # constraint labels on the left
    ubox(c, 10, 150, 100, 25, "PRIMARY KEY", bg=cd("#FFEBEE"), border=RED)
    ubox(c, 10, 115, 100, 25, "NOT NULL", bg=cd("#FFF9C4"), border=cd("#F9A825"))
    ubox(c, 10, 80, 100, 25, "UNIQUE", bg=cd("#E8EAF6"), border=PURP)
    ubox(c, 10, 45, 100, 25, "CHECK", bg=LGRN, border=GRN)
    arr(c, 110, 162, 155, 162, lbl="", ly=0)
    arr(c, 110, 127, 155, 140, lbl="", ly=0)
    arr(c, 110, 92, 155, 120, lbl="", ly=0)
    arr(c, 110, 57, 155, 100, lbl="", ly=0)

story += diag_wrap(Diag(diag_ddl, 460, 200),
                   "Fig B.1 — employees table with all constraint types annotated")
story.append(sp(4))

story.append(step_label(5, "Behind the Scenes"))
story.append(Paragraph(
    "When you run CREATE TABLE the engine writes a row to its internal system catalog "
    "(e.g. pg_class in PostgreSQL, information_schema.tables). A PRIMARY KEY automatically "
    "creates a <b>B-tree index</b> on that column — this is why PK lookups are fast. "
    "A UNIQUE constraint also creates a B-tree index. A FOREIGN KEY constraint causes the "
    "engine to check the referenced table on every INSERT/UPDATE to the child table and on "
    "every DELETE/UPDATE to the parent table. ALTER TABLE on a large table can be expensive "
    "because some changes (e.g. adding a NOT NULL column without a default) require "
    "rewriting every row.", NR))
story.append(sp(4))

story.append(step_label(6, "Realistic Practical Example"))
story.append(cb("""
    -- Real schema: e-commerce product catalog
    CREATE TABLE categories (
        cat_id   INT         PRIMARY KEY,
        cat_name VARCHAR(60) NOT NULL UNIQUE
    );

    CREATE TABLE products (
        product_id   INT            PRIMARY KEY,
        cat_id       INT            NOT NULL,
        sku          VARCHAR(30)    UNIQUE NOT NULL,
        price        DECIMAL(10,2)  NOT NULL CHECK (price >= 0),
        stock_qty    INT            DEFAULT 0 CHECK (stock_qty >= 0),
        created_at   TIMESTAMP      DEFAULT CURRENT_TIMESTAMP,
        FOREIGN KEY (cat_id) REFERENCES categories(cat_id)
    );
"""))
story.append(sp(4))

story.append(step_label(7, "Common Mistakes and Misconceptions"))
story += [
    bullet("Using VARCHAR(MAX) everywhere. Always choose the smallest appropriate data type."),
    bullet("Forgetting NOT NULL on columns that should never be empty. NULL creeps in silently."),
    bullet("DROP TABLE without a backup. It is immediate and permanent in most engines."),
    bullet("Thinking ALTER TABLE is free. On large tables, adding/changing columns can lock the table for minutes."),
    bullet("Duplicate UNIQUE + PRIMARY KEY — PRIMARY KEY already implies UNIQUE."),
    bullet("FOREIGN KEY without an index on the referencing column — this causes slow DELETE on the parent."),
]
story.append(sp(6))
story.append(hr())

# ══════════════════════════════════════════════════════════════════════════
# C. DML
# ══════════════════════════════════════════════════════════════════════════
story.append(PageBreak())
story.append(sec('C', "DML — Data Manipulation Language", "INSERT · UPDATE · DELETE"))
story.append(sp(8))

story.append(step_label(1, "Plain English Explanation"))
story.append(Paragraph(
    "DML (Data Manipulation Language) is the subset of SQL used to add, change, and remove "
    "rows inside tables. The three statements are: <b>INSERT</b> (add new rows), <b>UPDATE</b> "
    "(change existing rows), and <b>DELETE</b> (remove rows). DML operates within transactions — "
    "changes can be rolled back with ROLLBACK if something goes wrong, or permanently committed "
    "with COMMIT. Unlike DDL, DML only touches data, not the schema.", NR))
story.append(sp(4))

story.append(step_label(2, "Real-World Analogy"))
story.append(Paragraph(
    "DML is the day-to-day work of filling in, correcting, and removing records in the office "
    "filing system. INSERT is adding a new folder. UPDATE is whiting out a wrong value and "
    "writing the correct one. DELETE is pulling the folder out and throwing it away. All changes "
    "happen in pencil until you say COMMIT — only then are they permanent.", NR))
story.append(sp(4))

story.append(step_label(3, "Minimal SQL Example"))
story.append(cb("""
    -- INSERT: single row
    INSERT INTO customers (customer_id, name, email)
    VALUES (1, 'Alice Smith', 'alice@example.com');

    -- INSERT: multiple rows
    INSERT INTO customers (customer_id, name, email) VALUES
        (2, 'Bob Jones',  'bob@example.com'),
        (3, 'Carol Lee',  'carol@example.com');

    -- UPDATE: give all customers in city 'London' a discount flag
    UPDATE customers
    SET    discount = TRUE
    WHERE  city = 'London';

    -- DELETE: remove inactive customers
    DELETE FROM customers
    WHERE  last_login < '2023-01-01';
"""))
story.append(sp(4))

story.append(step_label(4, "Diagram — DML Flow"))

def diag_dml(c):
    W, H = 460, 160
    diag_title(c, W, H, "DML: INSERT / UPDATE / DELETE Transaction Flow")
    # boxes
    ubox(c, 10, 70, 90, 50, "INSERT", bg=LGRN, border=GRN)
    ubox(c, 120, 70, 90, 50, "UPDATE", bg=LBLUE, border=BLUE)
    ubox(c, 230, 70, 90, 50, "DELETE", bg=cd("#FFEBEE"), border=RED)
    # arrow to transaction
    ubox(c, 355, 60, 95, 70, "Transaction", bg=cd("#FFF9C4"), border=cd("#F9A825"))
    note(c, 402, 115, "Buffer (RAM)", 6.5, cd("#F9A825"))
    note(c, 402, 104, "Undo Log", 6.5, cd("#F9A825"))
    note(c, 402, 93, "Redo Log", 6.5, cd("#F9A825"))
    arr(c, 100, 95, 355, 95, lbl="DML writes", ly=8)
    arr(c, 210, 95, 355, 95, lbl="", ly=0)
    arr(c, 320, 95, 355, 95, lbl="", ly=0)
    # COMMIT / ROLLBACK
    ubox(c, 355, 20, 45, 28, "COMMIT", bg=LGRN, border=GRN)
    ubox(c, 405, 20, 48, 28, "ROLLBACK", bg=cd("#FFEBEE"), border=RED)
    arr(c, 402, 60, 377, 48, lbl="", ly=0)
    arr(c, 402, 60, 428, 48, lbl="", ly=0)
    note(c, 375, 14, "permanent", 6.5, GRN)
    note(c, 428, 14, "undo all", 6.5, RED)

story += diag_wrap(Diag(diag_dml, 460, 160),
                   "Fig C.1 — DML statements write to a transaction buffer; COMMIT or ROLLBACK decides the outcome")
story.append(sp(4))

story.append(step_label(5, "Behind the Scenes"))
story.append(Paragraph(
    "Every DML statement in a transaction writes to the engine's <b>undo log</b> (stores the "
    "old values so ROLLBACK can restore them) and <b>redo log / WAL (Write-Ahead Log)</b> "
    "(stores the new values so changes survive a crash). The actual data pages are updated in "
    "the <b>buffer pool</b> (RAM). Pages are written to disk asynchronously by the background "
    "checkpoint process. On COMMIT, the engine flushes the redo log to disk and marks the "
    "transaction as committed. On crash recovery, the engine replays the redo log to reconstruct "
    "committed changes and uses the undo log to roll back uncommitted ones.", NR))
story.append(sp(4))

story.append(step_label(6, "Realistic Practical Example"))
story.append(cb("""
    -- Realistic: process an order placement atomically
    BEGIN;

    INSERT INTO orders (order_id, customer_id, total)
    VALUES (5001, 42, 299.99);

    INSERT INTO order_items (order_id, product_id, qty, unit_price)
    VALUES (5001, 101, 2, 99.99),
           (5001, 205, 1, 100.01);

    UPDATE products
    SET    stock_qty = stock_qty - 2
    WHERE  product_id = 101;

    UPDATE products
    SET    stock_qty = stock_qty - 1
    WHERE  product_id = 205;

    COMMIT;  -- all 4 statements succeed together or none do
"""))
story.append(sp(4))

story.append(step_label(7, "Common Mistakes and Misconceptions"))
story += [
    bullet("UPDATE or DELETE without a WHERE clause — updates/deletes EVERY row. Always double-check."),
    bullet("Not using transactions for multi-statement operations — partial failures leave data inconsistent."),
    bullet("INSERT without specifying column names — breaks silently when columns are reordered."),
    bullet("Thinking DELETE frees disk space immediately. Most engines mark rows as deleted; space is reclaimed later (VACUUM/OPTIMIZE)."),
    bullet("Using UPDATE to 'move' data — UPDATE changes values in place; it does not move rows between tables."),
]
story.append(sp(6))
story.append(hr())

# ══════════════════════════════════════════════════════════════════════════
# D. SELECT BASICS
# ══════════════════════════════════════════════════════════════════════════
story.append(PageBreak())
story.append(sec('D', "SELECT Basics", "WHERE · ORDER BY · LIMIT · Execution Order"))
story.append(sp(8))

story.append(step_label(1, "Plain English Explanation"))
story.append(Paragraph(
    "SELECT is the SQL statement for querying data. You specify <b>which columns</b> to return, "
    "<b>which table(s)</b> to read from, <b>filtering conditions</b> (WHERE), <b>sort order</b> "
    "(ORDER BY), and optionally <b>how many rows</b> to return (LIMIT). The crucial insight is "
    "that the <b>written order</b> of clauses (SELECT...FROM...WHERE...) is NOT the order the "
    "engine executes them. The <b>logical execution order</b> is: FROM → JOIN → WHERE → GROUP BY "
    "→ HAVING → SELECT → ORDER BY → LIMIT.", NR))
story.append(sp(4))

story.append(step_label(2, "Real-World Analogy"))
story.append(Paragraph(
    "SELECT is like asking a librarian a precise question: 'From the SCIENCE section (FROM), "
    "find all books published after 2020 (WHERE), sorted alphabetically by title (ORDER BY), "
    "and show me just the first 10 results (LIMIT). For each book, tell me only the title and "
    "author (SELECT columns).' The librarian doesn't show you titles first and then fetch books "
    "— they go to the section first, filter by date, sort, then give you the first 10.", NR))
story.append(sp(4))

story.append(step_label(3, "Minimal SQL Example"))
story.append(cb("""
    -- Basic SELECT with all clauses
    SELECT  name,
            email,
            salary
    FROM    employees
    WHERE   salary > 50000
      AND   dept_id = 3
    ORDER BY salary DESC
    LIMIT   10;

    -- Wildcard (avoid in production code)
    SELECT * FROM employees WHERE emp_id = 42;

    -- Aliases for readability
    SELECT  emp_id AS id,
            name   AS full_name
    FROM    employees;
"""))
story.append(sp(4))

story.append(step_label(4, "Diagram — Logical Execution Order"))

def diag_select_order(c):
    W, H = 460, 175
    diag_title(c, W, H, "SELECT Logical Execution Order (not written order)")
    steps = [
        ("1. FROM",     "Identify source tables",       BG),
        ("2. JOIN",     "Combine tables",               TEAL),
        ("3. WHERE",    "Filter rows",                  ORG),
        ("4. GROUP BY", "Bucket rows",                  PURP),
        ("5. HAVING",   "Filter groups",                RED),
        ("6. SELECT",   "Pick columns / expressions",   BLUE),
        ("7. ORDER BY", "Sort result set",              GRN),
        ("8. LIMIT",    "Truncate output",              DGRY),
    ]
    x = 10
    for i, (lbl, desc, col) in enumerate(steps):
        bx = 10 + i * 56
        ubox(c, bx, 80, 52, 50, lbl, bg=cd("#FAFBFC"), border=col)
        note(c, bx+26, 74, desc, 5.5, col)
        if i < len(steps)-1:
            arr(c, bx+52, 105, bx+56, 105, lbl="", ly=0)
    note(c, 230, 150, "Written order:  SELECT ... FROM ... WHERE ... GROUP BY ... HAVING ... ORDER BY ... LIMIT", 7, MGRY)
    note(c, 230, 138, "Execution order is different — FROM is always processed first", 7, RED)

story += diag_wrap(Diag(diag_select_order, 460, 175),
                   "Fig D.1 — The 8-step logical execution order of a SELECT statement")
story.append(sp(4))

story.append(step_label(5, "Behind the Scenes"))
story.append(Paragraph(
    "The query engine first parses the SQL into an <b>Abstract Syntax Tree (AST)</b>, then the "
    "<b>query planner / optimiser</b> rewrites it into the most efficient <b>execution plan</b>. "
    "The planner considers available indexes, table statistics (row counts, value distributions), "
    "and join strategies. It produces a plan like: 'Index seek on employees.salary, filter "
    "dept_id=3, sort top-10'. The plan can be inspected with EXPLAIN or EXPLAIN ANALYZE. "
    "WHERE clauses run before SELECT, so you <b>cannot</b> use a column alias defined in SELECT "
    "inside a WHERE clause — the alias does not exist yet at WHERE execution time.", NR))
story.append(sp(4))

story.append(step_label(6, "Realistic Practical Example"))
story.append(cb("""
    -- Find top 5 highest-paid employees in HR dept, hired after 2020
    SELECT  e.emp_id,
            e.name,
            e.salary,
            e.hire_date
    FROM    employees  AS e
    WHERE   e.dept_id = (
                SELECT dept_id FROM departments WHERE dept_name = 'HR'
            )
      AND   e.hire_date > '2020-01-01'
    ORDER BY e.salary DESC
    LIMIT 5;
"""))
story.append(sp(4))

story.append(step_label(7, "Common Mistakes and Misconceptions"))
story += [
    bullet("Using SELECT * in application code — schema changes silently break your app."),
    bullet("Referencing a SELECT alias in WHERE: SELECT salary*1.1 AS bonus FROM ... WHERE bonus > 5000 — FAILS because WHERE runs before SELECT."),
    bullet("Assuming ORDER BY without LIMIT is free — sorting large result sets is expensive."),
    bullet("LIMIT without ORDER BY gives unpredictable rows — the engine returns any N rows it finds first."),
    bullet("WHERE column = NULL — always wrong. Use WHERE column IS NULL (see Section H)."),
]
story.append(sp(6))
story.append(hr())

# ══════════════════════════════════════════════════════════════════════════
# E. AGGREGATE FUNCTIONS
# ══════════════════════════════════════════════════════════════════════════
story.append(PageBreak())
story.append(sec('E', "Aggregate Functions", "COUNT · SUM · AVG · MIN · MAX · GROUP BY · HAVING"))
story.append(sp(8))

story.append(step_label(1, "Plain English Explanation"))
story.append(Paragraph(
    "Aggregate functions collapse multiple rows into a single result. <b>COUNT(*)</b> counts rows; "
    "<b>SUM</b> adds values; <b>AVG</b> computes the mean; <b>MIN/MAX</b> find extremes. "
    "Used alone they produce one row for the whole table. Paired with <b>GROUP BY</b>, they "
    "produce one row per group (e.g. one row per department). <b>HAVING</b> filters groups "
    "after aggregation — it is the WHERE clause for groups, not individual rows.", NR))
story.append(sp(4))

story.append(step_label(2, "Real-World Analogy"))
story.append(Paragraph(
    "Imagine a spreadsheet of sales transactions. GROUP BY department is like inserting a "
    "subtotal row for each department. SUM(sales) totals each subtotal row. HAVING SUM(sales) "
    "> 100000 hides departments with low totals — like an Excel AutoFilter on the subtotal "
    "column. WHERE filters individual transaction rows before grouping; HAVING filters the "
    "subtotal rows after.", NR))
story.append(sp(4))

story.append(step_label(3, "Minimal SQL Example"))
story.append(cb("""
    -- Total employees and average salary per department
    SELECT   dept_id,
             COUNT(*)        AS headcount,
             AVG(salary)     AS avg_salary,
             MIN(salary)     AS min_salary,
             MAX(salary)     AS max_salary,
             SUM(salary)     AS total_payroll
    FROM     employees
    WHERE    hire_date > '2019-01-01'   -- filter rows BEFORE grouping
    GROUP BY dept_id
    HAVING   COUNT(*) > 5               -- filter groups AFTER aggregation
    ORDER BY avg_salary DESC;
"""))
story.append(sp(4))

story.append(step_label(4, "Diagram — GROUP BY Mechanics"))

def diag_groupby(c):
    W, H = 460, 195
    diag_title(c, W, H, "GROUP BY: Rows Bucketed into Groups then Aggregated")
    # raw rows
    note(c, 80, 180, "Raw rows (after WHERE)", 7.5, DGRY)
    rows = [
        ("dept_id=1", "salary=70k", LBLUE),
        ("dept_id=2", "salary=55k", LGRN),
        ("dept_id=1", "salary=80k", LBLUE),
        ("dept_id=3", "salary=60k", LPUR),
        ("dept_id=2", "salary=65k", LGRN),
        ("dept_id=1", "salary=75k", LBLUE),
    ]
    for i, (d, s, bg) in enumerate(rows):
        ubox(c, 10, 140 - i*22, 130, 18, d, bg=bg, border=MGRY)
        note(c, 75, 143 - i*22, s, 6.5, MGRY)
    arr(c, 145, 90, 185, 90, lbl="GROUP BY dept_id", ly=10)
    # groups
    note(c, 300, 180, "Groups + Aggregates", 7.5, DGRY)
    ubox(c, 190, 140, 120, 28, "dept_id=1", bg=LBLUE, border=BLUE)
    note(c, 250, 148, "COUNT=3  AVG=75k", 6.5, BLUE)
    ubox(c, 190, 100, 120, 28, "dept_id=2", bg=LGRN, border=GRN)
    note(c, 250, 108, "COUNT=2  AVG=60k", 6.5, GRN)
    ubox(c, 190, 60, 120, 28, "dept_id=3", bg=LPUR, border=PURP)
    note(c, 250, 68, "COUNT=1  AVG=60k", 6.5, PURP)
    arr(c, 310, 90, 350, 90, lbl="HAVING COUNT(*) > 1", ly=10)
    # after HAVING
    ubox(c, 355, 130, 95, 28, "dept_id=1", bg=LBLUE, border=BLUE)
    ubox(c, 355, 90, 95, 28, "dept_id=2", bg=LGRN, border=GRN)
    note(c, 402, 70, "dept_id=3 filtered", 6.5, RED)
    note(c, 402, 60, "out (count=1)", 6.5, RED)

story += diag_wrap(Diag(diag_groupby, 460, 195),
                   "Fig E.1 — GROUP BY buckets rows; HAVING filters buckets by aggregate result")
story.append(sp(4))

story.append(step_label(5, "Behind the Scenes"))
story.append(Paragraph(
    "After the WHERE filter, the engine sorts (or hash-buckets) rows by the GROUP BY columns. "
    "Each unique combination of GROUP BY values forms a group. The aggregate function is then "
    "computed for each group. <b>Important engine rule</b>: every column in the SELECT list "
    "must either appear in GROUP BY or be inside an aggregate function. This is because after "
    "grouping, a group represents multiple rows — referencing a non-grouped, non-aggregated "
    "column would be ambiguous (which row's value should be returned?). Some engines "
    "(MySQL in lenient mode) allow this but return an arbitrary value — a well-known trap.", NR))
story.append(sp(4))

story.append(step_label(6, "Realistic Practical Example"))
story.append(cb("""
    -- Monthly revenue and order count per product category
    SELECT   c.cat_name,
             DATE_FORMAT(o.order_date, '%Y-%m') AS month,
             COUNT(DISTINCT o.order_id)          AS orders,
             SUM(oi.qty * oi.unit_price)         AS revenue
    FROM     order_items   AS oi
    JOIN     orders        AS o  ON o.order_id  = oi.order_id
    JOIN     products      AS p  ON p.product_id = oi.product_id
    JOIN     categories    AS c  ON c.cat_id    = p.cat_id
    WHERE    o.order_date >= '2024-01-01'
    GROUP BY c.cat_name, DATE_FORMAT(o.order_date, '%Y-%m')
    HAVING   SUM(oi.qty * oi.unit_price) > 10000
    ORDER BY month, revenue DESC;
"""))
story.append(sp(4))

story.append(step_label(7, "Common Mistakes and Misconceptions"))
story += [
    bullet("WHERE vs HAVING confusion — WHERE filters rows (before grouping); HAVING filters groups (after aggregation)."),
    bullet("COUNT(column) vs COUNT(*) — COUNT(column) ignores NULLs; COUNT(*) counts all rows including NULLs."),
    bullet("Selecting a non-grouped column: SELECT name, dept_id, AVG(salary) FROM ... GROUP BY dept_id — 'name' is ambiguous, will error in strict mode."),
    bullet("AVG on integer columns may lose decimal precision in some engines — cast to DECIMAL first."),
    bullet("Using HAVING instead of WHERE for row-level filters — HAVING runs after aggregation, so it is slower for row filters."),
]
story.append(sp(6))
story.append(hr())

# ══════════════════════════════════════════════════════════════════════════
# F. JOINs
# ══════════════════════════════════════════════════════════════════════════
story.append(PageBreak())
story.append(sec('F', "JOINs", "INNER · LEFT · RIGHT · FULL OUTER · Engine Internals"))
story.append(sp(8))

story.append(step_label(1, "Plain English Explanation"))
story.append(Paragraph(
    "A JOIN combines rows from two (or more) tables based on a related column. The four main "
    "types: <b>INNER JOIN</b> — only rows with a match in BOTH tables; <b>LEFT JOIN</b> — all "
    "rows from the left table, matched rows from the right (NULLs for unmatched right); "
    "<b>RIGHT JOIN</b> — opposite of LEFT JOIN; <b>FULL OUTER JOIN</b> — all rows from both "
    "tables, NULLs where there is no match. The join condition (usually ON t1.id = t2.fk) "
    "specifies which column pairs to match.", NR))
story.append(sp(4))

story.append(step_label(2, "Real-World Analogy"))
story.append(Paragraph(
    "Two lists: List A = all employees. List B = all department records. INNER JOIN = employees "
    "who have a matching department (unassigned employees are excluded). LEFT JOIN = all "
    "employees, showing department info where it exists, blank otherwise. RIGHT JOIN = all "
    "departments, showing their employees (empty departments also appear). FULL OUTER JOIN = "
    "every employee and every department, with blanks on either side where there is no match.", NR))
story.append(sp(4))

story.append(step_label(3, "Minimal SQL Example"))
story.append(cb("""
    -- INNER JOIN: only employees WITH a department
    SELECT e.name, d.dept_name
    FROM   employees e
    INNER JOIN departments d ON e.dept_id = d.dept_id;

    -- LEFT JOIN: all employees, NULL dept_name for unassigned
    SELECT e.name, d.dept_name
    FROM   employees e
    LEFT JOIN departments d ON e.dept_id = d.dept_id;

    -- FULL OUTER JOIN: all employees + all departments
    SELECT e.name, d.dept_name
    FROM   employees e
    FULL OUTER JOIN departments d ON e.dept_id = d.dept_id;
"""))
story.append(sp(4))

story.append(step_label(4, "Diagram — JOIN Venn Diagrams"))

def diag_joins(c):
    W, H = 460, 200
    diag_title(c, W, H, "JOIN Types: Which Rows Are Included")
    import math
    r = 35
    cx_a, cx_b, cy = 38, 70, 105

    def draw_join(ox, oy, fill_a, fill_b, fill_both, label):
        # circle A
        c.setFillColor(cd("#BBDEFB") if fill_a else cd("#F5F5F5"))
        c.setStrokeColor(BLUE); c.setLineWidth(1)
        c.circle(ox+cx_a, oy+cy, r, fill=1, stroke=1)
        # circle B
        c.setFillColor(cd("#C8E6C9") if fill_b else cd("#F5F5F5"))
        c.setStrokeColor(GRN); c.setLineWidth(1)
        c.circle(ox+cx_b, oy+cy, r, fill=1, stroke=1)
        # intersection label
        c.setFont("Helvetica-Bold", 6.5)
        c.setFillColor(cd("#37474F"))
        c.drawCentredString(ox+(cx_a+cx_b)//2, oy+cy-2, "match")
        c.setFont("Helvetica-Bold", 6); c.setFillColor(BLUE)
        c.drawCentredString(ox+cx_a-20, oy+cy+r+8, "A")
        c.setFillColor(GRN)
        c.drawCentredString(ox+cx_b+20, oy+cy+r+8, "B")
        c.setFont("Helvetica-Bold", 8); c.setFillColor(DGRY)
        c.drawCentredString(ox+(cx_a+cx_b)//2, oy+cy-r-12, label)

    draw_join(10,  0, False, False, True,  "INNER JOIN")
    draw_join(125, 0, True,  False, True,  "LEFT JOIN")
    draw_join(240, 0, False, True,  True,  "RIGHT JOIN")
    draw_join(355, 0, True,  True,  True,  "FULL OUTER")

    note(c, 55,  20, "only matched rows", 6, MGRY)
    note(c, 170, 20, "all A rows", 6, MGRY)
    note(c, 285, 20, "all B rows", 6, MGRY)
    note(c, 405, 20, "all rows both sides", 6, MGRY)

story += diag_wrap(Diag(diag_joins, 460, 200),
                   "Fig F.1 — Venn diagram showing rows included by each JOIN type")
story.append(sp(4))

story.append(step_label(5, "Behind the Scenes — Join Algorithms"))
story.append(Paragraph(
    "The query optimiser chooses one of three join algorithms based on table sizes and available "
    "indexes:", NR))
story += [
    bullet("<b>Nested Loop Join</b> — for each row in the outer table, scan the inner table for matches. O(n*m). Good for small inner tables or when an index exists on the join column."),
    bullet("<b>Hash Join</b> — build a hash table from the smaller table, then probe it with each row from the larger table. O(n+m). Good for large tables with no useful index."),
    bullet("<b>Merge Join</b> — both tables must be sorted on the join column (or already indexed). Merges them like a zipper. O(n+m) but requires a sort step if not pre-sorted."),
]
story.append(Paragraph(
    "The optimiser picks based on cost estimates from table statistics. You can force a join "
    "algorithm with hints in some engines (e.g. MySQL: STRAIGHT_JOIN).", NR))
story.append(sp(4))

story.append(step_label(6, "Realistic Practical Example"))
story.append(cb("""
    -- Find customers who have NEVER placed an order (LEFT JOIN anti-pattern)
    SELECT   c.customer_id,
             c.name,
             c.email
    FROM     customers c
    LEFT JOIN orders o ON o.customer_id = c.customer_id
    WHERE    o.order_id IS NULL      -- no match in orders = never ordered
    ORDER BY c.name;

    -- Multi-table JOIN: order details with customer and product names
    SELECT   o.order_id,
             c.name         AS customer,
             p.sku          AS product,
             oi.qty,
             oi.unit_price
    FROM     orders o
    JOIN     customers   c ON c.customer_id = o.customer_id
    JOIN     order_items oi ON oi.order_id   = o.order_id
    JOIN     products    p ON p.product_id  = oi.product_id;
"""))
story.append(sp(4))

story.append(step_label(7, "Common Mistakes and Misconceptions"))
story += [
    bullet("Forgetting the ON condition — a JOIN without ON produces a CROSS JOIN (cartesian product: every row with every row)."),
    bullet("Confusing LEFT JOIN result: NULL in the right-side column means 'no match', not that the column value is NULL in the right table."),
    bullet("FULL OUTER JOIN is not supported in MySQL — use UNION of LEFT JOIN and RIGHT JOIN."),
    bullet("Joining on columns with different data types silently disables index usage in some engines."),
    bullet("Many JOINs on large tables without indexes on join columns cause full table scans — extremely slow."),
]
story.append(sp(6))
story.append(hr())

# ══════════════════════════════════════════════════════════════════════════
# G. SUBQUERIES
# ══════════════════════════════════════════════════════════════════════════
story.append(PageBreak())
story.append(sec('G', "Subqueries", "Correlated · Non-correlated · EXISTS vs IN"))
story.append(sp(8))

story.append(step_label(1, "Plain English Explanation"))
story.append(Paragraph(
    "A subquery is a SELECT statement nested inside another SQL statement. A "
    "<b>non-correlated subquery</b> runs once and returns a result that the outer query uses — "
    "it is independent of the outer query. A <b>correlated subquery</b> runs once per row of "
    "the outer query because it references a column from the outer query. <b>EXISTS</b> tests "
    "whether a subquery returns any rows at all (stops at the first match). <b>IN</b> tests "
    "whether a value matches any value in a list returned by a subquery.", NR))
story.append(sp(4))

story.append(step_label(2, "Real-World Analogy"))
story.append(Paragraph(
    "Non-correlated: 'Find employees who earn more than the <i>company average salary</i>'. "
    "The average is calculated once. Correlated: 'For each employee, find their "
    "<i>department's</i> average salary' — each employee triggers a separate average "
    "calculation for their own department. EXISTS is like checking 'does this employee have "
    "at least one order?' — you stop searching as soon as you find the first one.", NR))
story.append(sp(4))

story.append(step_label(3, "Minimal SQL Example"))
story.append(cb("""
    -- Non-correlated: employees earning above company average
    SELECT name, salary
    FROM   employees
    WHERE  salary > (SELECT AVG(salary) FROM employees);

    -- Correlated: employees earning above their OWN dept average
    SELECT e.name, e.salary, e.dept_id
    FROM   employees e
    WHERE  e.salary > (
        SELECT AVG(e2.salary)
        FROM   employees e2
        WHERE  e2.dept_id = e.dept_id   -- references outer query
    );

    -- EXISTS: customers who placed at least one order
    SELECT c.name
    FROM   customers c
    WHERE  EXISTS (
        SELECT 1 FROM orders o WHERE o.customer_id = c.customer_id
    );
"""))
story.append(sp(4))

story.append(step_label(4, "Diagram — Correlated vs Non-correlated"))

def diag_subquery(c):
    W, H = 460, 175
    diag_title(c, W, H, "Non-Correlated vs Correlated Subquery Execution")
    # Non-correlated
    note(c, 115, 165, "Non-Correlated Subquery", 8, BG)
    ubox(c, 10, 90, 130, 60, "Outer Query", bg=LBLUE, border=BLUE)
    note(c, 75, 120, "WHERE salary > ?", 7, MGRY)
    ubox(c, 10, 20, 130, 50, "Inner Subquery", bg=LGRN, border=GRN)
    note(c, 75, 48, "SELECT AVG(salary)", 7, MGRY)
    note(c, 75, 38, "runs ONCE", 7, GRN)
    arr(c, 75, 70, 75, 90, lbl="result passed up", lx=30, ly=5)
    # Correlated
    note(c, 345, 165, "Correlated Subquery", 8, BG)
    ubox(c, 240, 90, 130, 60, "Outer Query", bg=LBLUE, border=BLUE)
    note(c, 305, 120, "each row of emp e", 7, MGRY)
    ubox(c, 240, 20, 130, 50, "Inner Subquery", bg=cd("#FFEBEE"), border=RED)
    note(c, 305, 48, "WHERE dept_id = e.dept_id", 7, MGRY)
    note(c, 305, 38, "runs N times (per row)", 7, RED)
    arr(c, 305, 70, 305, 90, lbl="ref outer col", lx=30, ly=5)
    arr(c, 305, 90, 305, 70, lbl="", ly=0)
    note(c, 195, 80, "vs", 10, DGRY)

story += diag_wrap(Diag(diag_subquery, 460, 175),
                   "Fig G.1 — Non-correlated runs once; correlated runs once per outer row")
story.append(sp(4))

story.append(step_label(5, "Behind the Scenes"))
story.append(Paragraph(
    "The optimiser often rewrites subqueries into JOINs internally for performance. A "
    "correlated subquery that the optimiser cannot rewrite runs N times (once per outer row) "
    "which is O(n*m) — slow on large tables. EXISTS with a correlated subquery short-circuits "
    "after the first match, making it faster than IN for large lists. IN vs EXISTS behaviour "
    "with NULLs also differs: if the IN subquery returns any NULL, the overall result can be "
    "UNKNOWN (not FALSE) — a subtle trap covered in Section H.", NR))
story.append(sp(4))

story.append(step_label(6, "Realistic Practical Example"))
story.append(cb("""
    -- NOT EXISTS anti-join: customers with no orders this year
    SELECT c.customer_id, c.name
    FROM   customers c
    WHERE  NOT EXISTS (
        SELECT 1
        FROM   orders o
        WHERE  o.customer_id = c.customer_id
          AND  o.order_date >= '2024-01-01'
    );

    -- IN with subquery: products in top-selling categories
    SELECT product_id, sku, price
    FROM   products
    WHERE  cat_id IN (
        SELECT   cat_id
        FROM     order_items oi
        JOIN     products p ON p.product_id = oi.product_id
        GROUP BY cat_id
        HAVING   SUM(oi.qty) > 500
    );
"""))
story.append(sp(4))

story.append(step_label(7, "Common Mistakes and Misconceptions"))
story += [
    bullet("Using a correlated subquery in SELECT for every row: SELECT name, (SELECT dept_name FROM ... WHERE ...) — runs N times; use a JOIN instead."),
    bullet("NOT IN with a subquery that can return NULL — if the subquery returns even one NULL, NOT IN returns no rows. Use NOT EXISTS instead."),
    bullet("Forgetting that IN is equivalent to = ANY(...); if performance is critical, a JOIN is usually faster."),
    bullet("Nesting subqueries 3+ levels deep — hard to read and optimise; refactor with CTEs (WITH clause)."),
]
story.append(sp(6))
story.append(hr())

# ══════════════════════════════════════════════════════════════════════════
# H. NULL HANDLING
# ══════════════════════════════════════════════════════════════════════════
story.append(PageBreak())
story.append(sec('H', "NULL Handling", "IS NULL · COALESCE · NULLIF · Three-Valued Logic"))
story.append(sp(8))

story.append(step_label(1, "Plain English Explanation"))
story.append(Paragraph(
    "NULL in SQL means 'unknown' or 'not applicable' — it is not zero, not an empty string, "
    "not false. NULL is a missing value. Because of this, SQL uses <b>three-valued logic</b>: "
    "every comparison evaluates to TRUE, FALSE, or <b>UNKNOWN</b>. Any arithmetic or comparison "
    "involving NULL produces UNKNOWN (or NULL). WHERE and JOIN conditions only pass rows where "
    "the condition is TRUE — UNKNOWN rows are silently dropped. Use <b>IS NULL</b> / "
    "<b>IS NOT NULL</b> to test for NULL. <b>COALESCE</b> returns the first non-NULL value "
    "in a list. <b>NULLIF</b> returns NULL if two values are equal.", NR))
story.append(sp(4))

story.append(step_label(2, "Real-World Analogy"))
story.append(Paragraph(
    "NULL is like an answer of 'I don't know' on a form. If you ask 'Is this person's age "
    "greater than 30?' and the age field is blank (unknown), the honest answer is 'I don't "
    "know' — not yes, not no. SQL treats that 'I don't know' as UNKNOWN, and UNKNOWN rows "
    "are excluded from results. This is why NULL != NULL — asking 'Is Unknown equal to "
    "Unknown?' also returns 'I don't know'.", NR))
story.append(sp(4))

story.append(step_label(3, "Minimal SQL Example"))
story.append(cb("""
    -- Wrong: this returns NO rows, even where phone IS NULL
    SELECT * FROM employees WHERE phone = NULL;

    -- Correct: use IS NULL
    SELECT * FROM employees WHERE phone IS NULL;

    -- COALESCE: use 'N/A' if phone is NULL
    SELECT name, COALESCE(phone, 'N/A') AS phone_display
    FROM   employees;

    -- NULLIF: treat empty string as NULL
    SELECT name, NULLIF(phone, '') AS phone
    FROM   employees;

    -- NULL in aggregates: COUNT ignores NULLs
    SELECT COUNT(phone)  AS with_phone,   -- ignores NULLs
           COUNT(*)      AS total_rows    -- counts all rows
    FROM   employees;
"""))
story.append(sp(4))

story.append(step_label(4, "Diagram — Three-Valued Logic Truth Table"))

def diag_null(c):
    W, H = 460, 200
    diag_title(c, W, H, "NULL: Three-Valued Logic and Common Traps")
    # truth table
    headers = ["A", "B", "A AND B", "A OR B", "NOT A"]
    vals = [
        ("TRUE",    "TRUE",    "TRUE",    "TRUE",    "FALSE"),
        ("TRUE",    "FALSE",   "FALSE",   "TRUE",    "FALSE"),
        ("TRUE",    "NULL",    "UNKNOWN", "TRUE",    "FALSE"),
        ("FALSE",   "NULL",    "FALSE",   "UNKNOWN", "TRUE"),
        ("NULL",    "NULL",    "UNKNOWN", "UNKNOWN", "UNKNOWN"),
    ]
    col_w = [1.6*cm, 1.6*cm, 2.2*cm, 2.2*cm, 1.8*cm]
    data = [[Paragraph(f"<b>{h}</b>", CH) for h in headers]]
    row_bg = {
        "TRUE": LGRN, "FALSE": cd("#FFEBEE"), "UNKNOWN": cd("#FFF9C4")
    }
    for row in vals:
        tr = []
        for cell in row:
            bg = row_bg.get(cell, WHT)
            tr.append(Paragraph(cell, St("Normal", fontSize=8, alignment=TA_CENTER,
                                         fontName="Courier" if cell=="UNKNOWN" else "Helvetica")))
        data.append(tr)
    tbl = Table(data, colWidths=col_w)
    tbl.setStyle(TableStyle([
        ("BACKGROUND",(0,0),(-1,0),BG),
        ("ROWBACKGROUNDS",(0,1),(-1,-1),[GRY, WHT]),
        ("GRID",(0,0),(-1,-1),0.4,BDR),
        ("ALIGN",(0,0),(-1,-1),"CENTER"),
        ("TOPPADDING",(0,0),(-1,-1),3),("BOTTOMPADDING",(0,0),(-1,-1),3),
    ]))
    # Draw table via canvas — use a Table as a Flowable embedded in the diagram
    # Instead: position as text notes
    note(c, 230, 185, "A      B      A AND B     A OR B    NOT A", 7, DGRY)
    note(c, 230, 172, "TRUE   TRUE   TRUE        TRUE      FALSE", 7, GRN)
    note(c, 230, 160, "TRUE   FALSE  FALSE       TRUE      FALSE", 7, MGRY)
    note(c, 230, 148, "TRUE   NULL   UNKNOWN     TRUE      FALSE", 7, ORG)
    note(c, 230, 136, "FALSE  NULL   FALSE       UNKNOWN   TRUE", 7, ORG)
    note(c, 230, 124, "NULL   NULL   UNKNOWN     UNKNOWN   UNKNOWN", 7, RED)
    # common trap
    ubox(c, 10, 80, 195, 30, "NULL = NULL", bg=cd("#FFEBEE"), border=RED)
    note(c, 107, 87, "evaluates to UNKNOWN (not TRUE!)", 7, RED)
    ubox(c, 215, 80, 235, 30, "NULL IS NULL", bg=LGRN, border=GRN)
    note(c, 332, 87, "evaluates to TRUE -- use this form", 7, GRN)
    ubox(c, 10, 40, 195, 30, "WHERE salary > NULL", bg=cd("#FFEBEE"), border=RED)
    note(c, 107, 47, "UNKNOWN -- row is excluded silently", 7, RED)
    ubox(c, 215, 40, 235, 30, "WHERE salary IS NOT NULL", bg=LGRN, border=GRN)
    note(c, 332, 47, "correct pattern", 7, GRN)

story += diag_wrap(Diag(diag_null, 460, 200),
                   "Fig H.1 — Three-valued logic truth table and NULL trap examples")
story.append(sp(4))

story.append(step_label(5, "Behind the Scenes"))
story.append(Paragraph(
    "NULL is stored in a special per-row <b>null bitmap</b> — a bitmask at the start of each "
    "row that records which columns are NULL. No actual value is stored for NULL columns. "
    "In a B-tree index, NULLs are indexed in PostgreSQL (at the beginning or end) but "
    "NOT indexed in Oracle (NULLs are excluded from B-tree indexes). This means "
    "WHERE col IS NULL does a full table scan in Oracle unless a special index is created. "
    "JOIN conditions with NULLs never match — two NULL foreign keys do not join together "
    "because NULL != NULL in three-valued logic.", NR))
story.append(sp(4))

story.append(step_label(6, "Realistic Practical Example"))
story.append(cb("""
    -- Report: show 0 if no sales, not NULL
    SELECT   e.name,
             COALESCE(SUM(s.amount), 0) AS total_sales
    FROM     employees e
    LEFT JOIN sales s ON s.emp_id = e.emp_id
    GROUP BY e.emp_id, e.name;

    -- Safe division: avoid divide-by-zero using NULLIF
    SELECT   dept_id,
             total_payroll / NULLIF(headcount, 0) AS avg_salary
    FROM     dept_summary;

    -- Find rows where optional field was never filled in
    SELECT name FROM employees
    WHERE  manager_id IS NULL;   -- top-level managers
"""))
story.append(sp(4))

story.append(step_label(7, "Common Mistakes and Misconceptions"))
story += [
    bullet("WHERE col = NULL — always returns 0 rows. Always use IS NULL."),
    bullet("NOT IN subquery returning NULLs — NOT IN (1, 2, NULL) returns UNKNOWN for everything. Use NOT EXISTS."),
    bullet("Believing NULLs are excluded from aggregates — AVG, SUM, COUNT(col) all ignore NULLs; only COUNT(*) includes them."),
    bullet("NULL in a UNIQUE constraint — most databases allow multiple NULLs in a UNIQUE column (because NULL != NULL)."),
    bullet("NULL in string concatenation: 'Hello' || NULL = NULL in SQL standard — use COALESCE to guard."),
]
story.append(sp(6))
story.append(hr())

# ══════════════════════════════════════════════════════════════════════════
# I. CONSTRAINTS AND REFERENTIAL INTEGRITY
# ══════════════════════════════════════════════════════════════════════════
story.append(PageBreak())
story.append(sec('I', "Constraints & Referential Integrity", "CASCADE DELETE · CASCADE UPDATE · Constraint Types"))
story.append(sp(8))

story.append(step_label(1, "Plain English Explanation"))
story.append(Paragraph(
    "Referential integrity means the database engine enforces that foreign key values always "
    "point to an existing row in the referenced table. You cannot insert an order with a "
    "customer_id that does not exist. <b>CASCADE DELETE</b> automatically deletes child rows "
    "when the parent row is deleted. <b>CASCADE UPDATE</b> propagates a primary key change to "
    "all foreign key references. Alternatives are <b>SET NULL</b> (set FK to NULL on parent "
    "delete), <b>SET DEFAULT</b> (set FK to its default value), and <b>RESTRICT / NO ACTION</b> "
    "(block the parent DELETE/UPDATE if child rows exist).", NR))
story.append(sp(4))

story.append(step_label(2, "Real-World Analogy"))
story.append(Paragraph(
    "Referential integrity is the rule that a company cannot delete an employee file while "
    "that employee is still listed as the manager of active projects. CASCADE DELETE is like "
    "a policy: 'When we delete a department, automatically fire all its employees'. SET NULL "
    "is: 'When the department is dissolved, leave the employees but set their department field "
    "to blank'. RESTRICT is: 'Block the department deletion until all employees are "
    "reassigned first'.", NR))
story.append(sp(4))

story.append(step_label(3, "Minimal SQL Example"))
story.append(cb("""
    CREATE TABLE departments (
        dept_id   INT         PRIMARY KEY,
        dept_name VARCHAR(80) NOT NULL
    );

    CREATE TABLE employees (
        emp_id  INT PRIMARY KEY,
        name    VARCHAR(100) NOT NULL,
        dept_id INT,
        FOREIGN KEY (dept_id) REFERENCES departments(dept_id)
            ON DELETE CASCADE      -- delete employee when dept deleted
            ON UPDATE CASCADE      -- update FK when dept_id changes
    );

    -- Delete dept 10: automatically deletes all employees in dept 10
    DELETE FROM departments WHERE dept_id = 10;

    -- With SET NULL: employee stays, dept_id becomes NULL
    -- FOREIGN KEY (dept_id) REFERENCES departments(dept_id) ON DELETE SET NULL
"""))
story.append(sp(4))

story.append(step_label(4, "Diagram — Referential Integrity Actions"))

def diag_cascade(c):
    W, H = 460, 190
    diag_title(c, W, H, "Referential Integrity: Parent DELETE Actions")
    # Parent
    ubox(c, 10, 110, 130, 55, "departments", bg=LBLUE, border=BLUE)
    note(c, 75, 150, "dept_id = 10 (deleted)", 7, RED)
    # Actions
    actions = [
        ("CASCADE DELETE",  "child rows deleted",       LGRN,            GRN,   90),
        ("SET NULL",        "FK set to NULL",           cd("#FFF9C4"),   ORG,  155),
        ("RESTRICT",        "DELETE blocked!",          cd("#FFEBEE"),   RED,  225),
        ("SET DEFAULT",     "FK set to default value",  LPUR,            PURP, 295),
    ]
    for label, desc, bg, border, rx in actions:
        ubox(c, rx, 100, 115, 40, label, bg=bg, border=border)
        note(c, rx+57, 94, desc, 6.5, border)
        arr(c, 140, 137, rx, 120, lbl="", ly=0)
    # employees child
    note(c, 75, 105, "FK: dept_id", 7, MGRY)

story += diag_wrap(Diag(diag_cascade, 460, 190),
                   "Fig I.1 — Four referential integrity actions when a parent row is deleted")
story.append(sp(4))

story.append(step_label(5, "Behind the Scenes"))
story.append(Paragraph(
    "When you DELETE a parent row, the engine checks the FK constraint index to find child "
    "rows. For CASCADE DELETE it issues internal DELETE statements for those child rows "
    "(recursively — grandchildren are also cascaded). For RESTRICT / NO ACTION the engine "
    "raises an error before the delete completes. Note: <b>NO ACTION</b> is deferred (checked "
    "at transaction end in some engines) while RESTRICT is immediate. CASCADE is powerful but "
    "dangerous — an accidental delete of a top-level row can wipe thousands of child rows "
    "across multiple tables. Always use CASCADE with careful application logic.", NR))
story.append(sp(4))

story.append(step_label(6, "Realistic Practical Example"))
story.append(cb("""
    -- E-commerce: order_items cascade-deleted with their order
    CREATE TABLE orders (
        order_id    INT PRIMARY KEY,
        customer_id INT NOT NULL,
        FOREIGN KEY (customer_id) REFERENCES customers(customer_id)
            ON DELETE RESTRICT    -- cannot delete customer with orders
    );

    CREATE TABLE order_items (
        item_id    INT PRIMARY KEY,
        order_id   INT NOT NULL,
        product_id INT NOT NULL,
        qty        INT NOT NULL DEFAULT 1,
        FOREIGN KEY (order_id) REFERENCES orders(order_id)
            ON DELETE CASCADE,    -- delete items when order deleted
        FOREIGN KEY (product_id) REFERENCES products(product_id)
            ON DELETE RESTRICT    -- cannot delete product with order history
    );
"""))
story.append(sp(4))

story.append(step_label(7, "Common Mistakes and Misconceptions"))
story += [
    bullet("Using CASCADE DELETE everywhere without thinking — a single top-level DELETE can wipe entire subtrees of data."),
    bullet("Not indexing the foreign key column — without an index, checking/cascading child rows requires a full table scan."),
    bullet("Thinking RESTRICT and NO ACTION are identical — in PostgreSQL, NO ACTION is deferred to end-of-transaction; RESTRICT is immediate."),
    bullet("Circular foreign keys — Table A FK to B, B FK to A — engine usually disallows or requires deferred checking."),
    bullet("Soft-delete pattern with CASCADE — if you use a 'deleted_at' column instead of actual deletes, CASCADE doesn't fire."),
]
story.append(sp(6))
story.append(hr())

# ══════════════════════════════════════════════════════════════════════════
# J. VIEWS
# ══════════════════════════════════════════════════════════════════════════
story.append(PageBreak())
story.append(sec('J', "Views", "What They Are · When to Use · Updatable vs Non-updatable"))
story.append(sp(8))

story.append(step_label(1, "Plain English Explanation"))
story.append(Paragraph(
    "A VIEW is a named, stored SELECT query. It looks and behaves like a table from the "
    "caller's perspective, but it has no data of its own — when you query a view, the engine "
    "runs the underlying SELECT query and returns the results. Views serve as a "
    "<b>security layer</b> (expose only certain columns to certain users), a "
    "<b>simplification layer</b> (hide complex JOINs behind a simple name), and an "
    "<b>abstraction layer</b> (if the underlying table changes, only the view definition "
    "needs updating — application code still queries the view by name). An "
    "<b>updatable view</b> allows INSERT/UPDATE/DELETE to pass through to the underlying "
    "table. A view is <b>non-updatable</b> if it contains GROUP BY, DISTINCT, aggregates, "
    "UNION, or references to multiple base tables in certain ways.", NR))
story.append(sp(4))

story.append(step_label(2, "Real-World Analogy"))
story.append(Paragraph(
    "A view is like a window in an office. The window shows a specific part of the room (the "
    "underlying data). Different windows can show the same room from different angles — a "
    "HR view shows only salary and department data; a finance view shows only amounts. If the "
    "furniture in the room changes (table schema), you may need to adjust the window (update "
    "the view definition), but everyone looking through a window sees the right picture "
    "without knowing the full room layout.", NR))
story.append(sp(4))

story.append(step_label(3, "Minimal SQL Example"))
story.append(cb("""
    -- Create a view: simple column subset (updatable)
    CREATE VIEW public_employees AS
    SELECT emp_id, name, dept_id, hire_date
    FROM   employees;               -- no salary exposed

    -- Query the view like a table
    SELECT * FROM public_employees WHERE dept_id = 3;

    -- Create a non-updatable view: uses aggregates
    CREATE VIEW dept_stats AS
    SELECT   dept_id,
             COUNT(*)    AS headcount,
             AVG(salary) AS avg_salary
    FROM     employees
    GROUP BY dept_id;

    -- Drop a view
    DROP VIEW public_employees;
"""))
story.append(sp(4))

story.append(step_label(4, "Diagram — View Architecture"))

def diag_view(c):
    W, H = 460, 185
    diag_title(c, W, H, "View: Stored Query as a Virtual Table")
    # base table
    ubox(c, 10, 70, 130, 90, "employees table", bg=LBLUE, border=BLUE)
    note(c, 75, 147, "emp_id", 7, MGRY)
    note(c, 75, 136, "name", 7, MGRY)
    note(c, 75, 125, "salary  [hidden]", 7, RED)
    note(c, 75, 114, "dept_id", 7, MGRY)
    note(c, 75, 103, "hire_date", 7, MGRY)
    note(c, 75, 92, "ssn     [hidden]", 7, RED)
    # view definition
    ubox(c, 170, 90, 130, 55, "public_employees", bg=LGRN, border=GRN)
    note(c, 235, 132, "VIEW = stored SELECT", 7, GRN)
    note(c, 235, 121, "emp_id, name", 7, MGRY)
    note(c, 235, 110, "dept_id, hire_date", 7, MGRY)
    arr(c, 140, 115, 170, 115, lbl="SELECT subset", ly=8)
    # app queries view
    ubox(c, 330, 90, 120, 55, "Application", bg=LPUR, border=PURP)
    note(c, 390, 132, "SELECT * FROM", 7, MGRY)
    note(c, 390, 121, "public_employees", 7, PURP)
    note(c, 390, 110, "salary never exposed", 7, RED)
    arr(c, 300, 117, 330, 117, lbl="query view", ly=8)
    arr(c, 330, 117, 300, 117, lbl="results", ly=-5)
    # note on non-updatable
    note(c, 230, 70, "Updatable: no GROUP BY/DISTINCT/aggregates in single-table view", 7, GRN)
    note(c, 230, 58, "Non-updatable: GROUP BY, UNION, aggregates, multi-table complex joins", 7, RED)

story += diag_wrap(Diag(diag_view, 460, 185),
                   "Fig J.1 — A view acts as a virtual table; the engine runs the underlying SELECT on query")
story.append(sp(4))

story.append(step_label(5, "Behind the Scenes"))
story.append(Paragraph(
    "A view definition is stored in the system catalog. When you query a view, the engine "
    "performs <b>view expansion</b>: it substitutes the view's SELECT in place of the view "
    "name in your query, then optimises the combined query as a single statement. There is no "
    "separate caching of view results (unless you use a <b>MATERIALISED VIEW</b>, which stores "
    "a physical snapshot of the query result and must be explicitly refreshed). Updatable views "
    "work through a set of strict rules the engine checks: single base table, no aggregates, "
    "no DISTINCT, no subqueries in the SELECT list, all NOT NULL columns without defaults "
    "are in the view.", NR))
story.append(sp(4))

story.append(step_label(6, "Realistic Practical Example"))
story.append(cb("""
    -- Security view: HR can see salary; public cannot
    CREATE VIEW employee_public AS
    SELECT emp_id, name, dept_id, hire_date
    FROM   employees;

    CREATE VIEW employee_hr AS
    SELECT emp_id, name, dept_id, hire_date, salary, manager_id
    FROM   employees;

    -- Convenience view: denormalised order summary
    CREATE VIEW order_summary AS
    SELECT  o.order_id,
            c.name          AS customer_name,
            o.order_date,
            SUM(oi.qty * oi.unit_price) AS total_amount
    FROM    orders o
    JOIN    customers   c  ON c.customer_id = o.customer_id
    JOIN    order_items oi ON oi.order_id   = o.order_id
    GROUP BY o.order_id, c.name, o.order_date;
    -- This view is NOT updatable due to GROUP BY
"""))
story.append(sp(4))

story.append(step_label(7, "Common Mistakes and Misconceptions"))
story += [
    bullet("Thinking a view caches data — a regular view re-runs its query every time. For caching use MATERIALISED VIEW."),
    bullet("Complex views inside views (view stacking) — the engine must expand all layers; deeply nested views are hard to optimise."),
    bullet("Assuming views always improve performance — a view with joins can be slower than a direct query if the optimiser cannot push down filters."),
    bullet("Updating a view that joins two tables — UPDATE through a join view is not supported in most engines; use INSTEAD OF triggers."),
    bullet("Forgetting to add WITH CHECK OPTION — without it, INSERTs/UPDATEs through a view can create rows invisible to the view."),
]
story.append(sp(6))
story.append(hr())

# ══════════════════════════════════════════════════════════════════════════
# CHEAT SHEET
# ══════════════════════════════════════════════════════════════════════════
story.append(PageBreak())
story.append(Paragraph("SQL Fundamentals — Quick Reference Cheat Sheet", H1))
story.append(sp(6))

story.append(cheat_table(
    ["Statement / Concept", "Syntax / Rule", "Key Gotcha"],
    [
        ["CREATE TABLE",
         "CREATE TABLE t (col TYPE CONSTRAINT, ...);",
         "PRIMARY KEY auto-creates B-tree index"],
        ["ALTER TABLE",
         "ALTER TABLE t ADD COLUMN col TYPE;",
         "On large tables this can rewrite all rows"],
        ["INSERT",
         "INSERT INTO t (cols) VALUES (...);",
         "Always list column names explicitly"],
        ["UPDATE",
         "UPDATE t SET col=val WHERE condition;",
         "Omit WHERE = update every row"],
        ["DELETE",
         "DELETE FROM t WHERE condition;",
         "Omit WHERE = delete every row"],
        ["SELECT execution order",
         "FROM > JOIN > WHERE > GROUP BY > HAVING > SELECT > ORDER BY > LIMIT",
         "Alias in SELECT not visible in WHERE"],
        ["INNER JOIN",
         "JOIN t2 ON t1.id = t2.fk",
         "Omit ON = cartesian product"],
        ["LEFT JOIN",
         "LEFT JOIN t2 ON ...",
         "NULL in right cols = no match"],
        ["GROUP BY",
         "SELECT col, AGG() FROM t GROUP BY col",
         "All non-agg SELECT cols must be in GROUP BY"],
        ["HAVING",
         "HAVING AGG() > value",
         "HAVING filters groups; WHERE filters rows"],
        ["NULL check",
         "WHERE col IS NULL / IS NOT NULL",
         "col = NULL always returns UNKNOWN"],
        ["COALESCE",
         "COALESCE(col, default_val)",
         "Returns first non-NULL value in list"],
        ["EXISTS",
         "WHERE EXISTS (SELECT 1 FROM ...)",
         "Stops at first match; safe with NULLs"],
        ["NOT IN + NULL",
         "Avoid NOT IN (subquery with NULLs)",
         "Returns 0 rows if subquery has any NULL"],
        ["CASCADE DELETE",
         "ON DELETE CASCADE",
         "Recursively deletes child rows"],
        ["View",
         "CREATE VIEW v AS SELECT ...;",
         "Non-updatable if GROUP BY/aggregates present"],
    ]
))
story.append(sp(12))

# ══════════════════════════════════════════════════════════════════════════
# BUILD
# ══════════════════════════════════════════════════════════════════════════
doc.build(story)
print(f"PDF written: {OUT}")
sz = os.path.getsize(OUT)
print(f"File size:   {sz:,} bytes ({sz/1024:.1f} KB)")
if sz < 20_000:
    print("WARNING: file is under 20 KB")
else:
    print("OK: file exceeds 20 KB threshold")
