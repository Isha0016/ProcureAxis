const fs = require("fs");

const inputFile = "FSC Backend End-to-End Demo.postman_collection.json";
const outputFile = "FSC-API-Collection-Safe.json";

const sensitiveKey =
    /token|password|secret|api[_-]?key|authorization|cookie|credential/i;

function sanitize(data) {
    if (Array.isArray(data)) {
        return data.map(sanitize);
    }

    if (data && typeof data === "object") {
        const result = {};

        for (const [key, value] of Object.entries(data)) {
            if (sensitiveKey.test(key)) {
                result[key] = "REPLACE_WITH_YOUR_CREDENTIAL";
            } else {
                result[key] = sanitize(value);
            }
        }

        return result;
    }

    // Check JSON stored as text, such as raw request bodies.
    if (typeof data === "string") {
        try {
            return JSON.stringify(sanitize(JSON.parse(data)));
        } catch {
            return data;
        }
    }

    return data;
}

try {
    const original = JSON.parse(
        fs.readFileSync(inputFile, "utf8")
    );

    const cleaned = sanitize(original);

    fs.writeFileSync(
        outputFile,
        JSON.stringify(cleaned, null, 2)
    );

    console.log("Safe collection created:", outputFile);
} catch (error) {
    console.error("Error:", error.message);
}