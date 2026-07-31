"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.RestGeminiClient = void 0;
const MODEL = "gemini-2.5-flash";
const API_BASE = "https://generativelanguage.googleapis.com/v1beta/models";
function toGeminiFunctionDeclarations(tools) {
    return tools.map((tool) => ({
        name: tool.name,
        description: tool.description,
        parameters: tool.parameters,
    }));
}
/**
 * Cliente real de Gemini (REST) detras de la interfaz GeminiClient. La orquestacion de
 * tools vive en aiAgent.ts (probada sin red); esta clase solo traduce el formato de
 * Gemini hacia/desde ese contrato, para poder swapear de proveedor sin tocar la logica.
 */
class RestGeminiClient {
    constructor(apiKey) {
        this.apiKey = apiKey;
    }
    async generateContent(params) {
        const contents = [{ role: "user", parts: [{ text: params.prompt }] }];
        if (params.functionResponses?.length) {
            contents.push({
                role: "function",
                parts: params.functionResponses.map((fr) => ({
                    functionResponse: { name: fr.name, response: fr.response },
                })),
            });
        }
        const response = await fetch(`${API_BASE}/${MODEL}:generateContent?key=${this.apiKey}`, {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({
                contents,
                tools: [{ functionDeclarations: toGeminiFunctionDeclarations(params.tools) }],
            }),
        });
        if (!response.ok) {
            throw new Error(`Gemini API respondio ${response.status}: ${await response.text()}`);
        }
        const body = (await response.json());
        const parts = body.candidates?.[0]?.content?.parts ?? [];
        const text = parts.find((part) => part.text)?.text;
        const functionCalls = parts
            .filter((part) => part.functionCall)
            .map((part) => part.functionCall);
        return {
            text,
            functionCalls: functionCalls.length > 0 ? functionCalls : undefined,
        };
    }
}
exports.RestGeminiClient = RestGeminiClient;
//# sourceMappingURL=geminiClient.js.map