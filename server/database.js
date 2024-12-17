import sqlite3 from "sqlite3";

const db = new sqlite3.Database("./questions.db", (err) => {
    if (err) {
        console.log(err.message);
    } else {
        console.log("Database connected");
        db.run(`
            CREATE TABLE IF NOT EXISTS questions 
            (id INTEGER PRIMARY KEY AUTOINCREMENT,
            lectureTitle TEXT,
            lectureContent TEXT,
            questionText TEXT,
            questionType TEXT,
            choices TEXT,
            correctAnswer TEXT,
            difficultyLevel TEXT,
            dateCreated TEXT,
            fileName TEXT)
            `, (err) => {
                if (err) {
                    console.log(err.message)
                }
            }); 
    }
});

export function saveQuestion(data) {
    const { fileName, lectureTitle, lectureContent, questionText, questionType, choices, correctAnswer, difficultyLevel, dateCreated } = data;

    db.run(`
        INSERT INTO questions (fileName, lectureTitle, lectureContent, questionText, questionType, choices, correctAnswer, difficultyLevel, dateCreated)
        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)`,
        [fileName, lectureTitle, lectureContent, questionText, questionType, JSON.stringify(choices), correctAnswer, difficultyLevel, dateCreated],
        function (err) {
            if (err) {
                return console.error("Error saving question", err.message);
            }
            console.log(`A row has been inserted with rowid ${this.lastID}`);
        }
    );
}

export function getAllQuestions(callback) {
    db.all("SELECT * FROM questions", [], (err, rows) => {
        if (err) {
            console.error("Error retrieving questions", err.message);
            callback(err, null);
        } else {
            callback(null, rows);
        }
    });
}

export function getAllQuestionsByFileName(fileName, callback) {
    db.all("SELECT * FROM questions WHERE fileName = ?", [fileName], (err, rows) => {
        if (err) {
            console.error("Error retrieving questions", err.message);
            callback(err, null);
        } else {
            callback(null, rows);
        }
    });
}

export function getResources(callback) {
    const query = `
        SELECT fileName, COUNT(*) as questionCount
        FROM questions
        GROUP BY fileName
        `;

    db.all(query, (err, rows) => {
        if (err) {
            console.error("Error retrieving resources", err.message);
            callback(err, null)
        } else {
            callback(null, rows);
        }
    });
}

export default db;
