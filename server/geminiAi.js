import { GoogleGenerativeAI } from "@google/generative-ai";
import { config } from "dotenv";
import { schema } from "./questionSchema.js";

config({ path: "./.env" });

const geminiApiKey = process.env.GEMINI_API_KEY;

export async function callGeminiAPI(prompt) {
    const genAi = new GoogleGenerativeAI(geminiApiKey);
    const model = genAi.getGenerativeModel({
        model: "gemini-1.5-flash",
        generationConfig: {
            responseMimeType: "application/json",
            responseSchema: schema,
        },
    });
    const result = await model.generateContent(prompt);
    console.log(result.response.text())
    return result.response.text();
}
