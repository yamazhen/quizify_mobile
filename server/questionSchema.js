import { SchemaType } from "@google/generative-ai";

export const schema = {
    description: "Schema for generating multiple-choice questions based on lecture resources",
    type: SchemaType.OBJECT,
    properties: {
        lectureTitle: {
            type: SchemaType.STRING,
            description: "Title of the lecture",
            nullable: false,
        },
        lectureContent: {
            type: SchemaType.STRING,
            description: "Main content of the lecture used as the basis for generating questions",
            nullable: false,
        },
        questions: {
            type: SchemaType.ARRAY,
            description: "List of multiple-choice questions generated from the lecture",
            items: {
                type: SchemaType.OBJECT,
                properties: {
                    questionText: {
                        type: SchemaType.STRING,
                        description: "The text of the question",
                        nullable: false,
                    },
                    questionType: {
                        type: SchemaType.STRING,
                        description: "Type of question, which is restricted to multiple-choice",
                        enum: ["multiple-choice"],
                        nullable: false,
                    },
                    choices: {
                        type: SchemaType.ARRAY,
                        description: "List of answer choices for the multiple-choice question",
                        items: {
                            type: SchemaType.STRING,
                        },
                        nullable: false,
                    },
                    correctAnswer: {
                        type: SchemaType.STRING,
                        description: "The correct answer for the question",
                        nullable: false,
                    },
                    difficultyLevel: {
                        type: SchemaType.STRING,
                        description: "Difficulty level of the question",
                        enum: ["easy", "medium", "hard"],
                        nullable: false,
                    },
                },
                required: ["questionText", "questionType", "choices", "correctAnswer", "difficultyLevel"],
            },
        },
        metadata: {
            type: SchemaType.OBJECT,
            description: "Additional metadata about the lecture or question generation process",
            properties: {
                dateCreated: {
                    type: SchemaType.STRING,
                    description: "Date when the lecture was added",
                    nullable: false,
                },
            },
            required: ["dateCreated"],
        },
    },
    required: ["lectureTitle", "lectureContent", "questions"],
};
