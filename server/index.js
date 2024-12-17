import express from "express";
import pdfParse from "pdf-parse";
import cors from "cors";
import multer from "multer";
import { getAllQuestions, getAllQuestionsByFileName, getResources, saveQuestion } from "./database.js";
import { callGeminiAPI } from './geminiAi.js';

const app = express();
const port = 3000;
const upload = multer();

app.use(cors({
    origin: "*",
}))

app.use(express.json());

app.post('/text', async (req,res) => {
    const { prompt } = req.body;
    try {
        const response = await callGeminiAPI(prompt)
        res.json({ response });
    } catch (error) {
        console.error(error);
        res.status(500).json({ error: "Something went wrong" });
    }
});

app.post('/upload-pdf', upload.single("pdf"), async (req,res) => {
    try {
        const fileName = req.headers['file-name'] || "unknown.pdf";
        const dataBuffer = await pdfParse(req.file.buffer);
        const extractedText = dataBuffer.text;
        const prompt = `Based on the following lecture content generate a list of questions.
            Lecture content: ${extractedText}.
            Please focus on the key concepts and important points from the lecture to create relevant questions. Provide 10 questions.`

        let response = await callGeminiAPI(prompt)

        if (typeof response === "string") {
            response = JSON.parse(response);
        }

        if (response && response.questions && Array.isArray(response.questions)) {
            for (const question of response.questions) {
                const questionData = {
                    fileName,
                    lectureTitle: response.lectureTitle,
                    lectureContent: response.lectureContent,
                    questionText: question.questionText,
                    questionType: question.questionType,
                    choices: question.choices,
                    correctAnswer: question.correctAnswer,
                    difficultyLevel: question.difficultyLevel,
                    dateCreated: new Date().toISOString()
                };
                saveQuestion(questionData);
            }
            res.json({ questions: response.questions });
        } else {
            res.status(500).json({ error: "Invalid response format. 'questions' field is missing or not an array." });
        }
    } catch (error) {
        console.error(error);
        res.status(500).json({ error: "Something went wrong" });
    }
});

app.get("/questions", (req, res) => {
    getAllQuestions((err, rows) => {
        if (err) {
            res.status(500).json({ error: "Could not retrieve questions" });
        } else {
            res.json({ questions: rows });
        }
    });
});

app.get("/questions/:fileName", (req, res) => {
    const fileName = req.params.fileName;

    getAllQuestionsByFileName(fileName, (err, rows) => {
        if (err) {
            res.status(500).json({ error: "Could not retrieve questions" });
        } else {
            res.json({ questions: rows });
        }
    });
});

app.get("/resources", (req,res) => {
    getResources((err, rows) => {
        if (err) {
            res.status(500).json({ error: "Could not retrieve resources" })
        } else {
            res.json({ questions: rows });
        }
    })
});

app.post("/submit-answers", (req,res) => {
    const userAnswers = req.body.answers;

    getAllQuestions((err, questions) => {
        if(err) {
            return res.status(500).json({ error:"Could not retrieve questions" })
        }

        const filteredQuestions = questions.filter(question => 
            userAnswers.some(answer => answer.questionId === question.id)
        );

        const results = filteredQuestions.map((question) => {
            const userAnswer = userAnswers.find((a) => a.questionId === question.id);
            const isCorrect = userAnswer && userAnswer.answer
                ? Array.isArray(userAnswer.answer)
                    ? userAnswer.answer.some((ans) => ans.trim().toLowerCase() === question.correctAnswer.trim().toLowerCase())
                    : userAnswer.answer.trim().toLowerCase() === question.correctAnswer.trim().toLowerCase()
                : false;
            return {
                questionId: question.id,
                questionText: question.questionText,
                userAnswer: userAnswer ? userAnswer.answer : null,
                correctAnswer: question.correctAnswer,
                isCorrect,
            };
        });

        const score = results.filter((result) => result.isCorrect).length;
        res.json({ results, score, totalQuestions: filteredQuestions.length });
    })
})


app.listen(port, () => {
    console.log(`Server is running on localhost:${port}`);
});
