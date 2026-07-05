// Full name letters: English, Hebrew (א–ת) and Russian/Cyrillic (Ѐ–ӿ).
// At least two words separated by single spaces, up to 30 characters total.
const NAME_LETTERS = "A-Za-zא-תЀ-ӿ";
const FULL_NAME_PATTERN = new RegExp(`^(?=.{1,30}$)[${NAME_LETTERS}]+(?: [${NAME_LETTERS}]+)+$`);
// Password: 2–10 chars from the allowed set, and must contain at least one English letter.
const PASSWORD_PATTERN = /^(?=.*[A-Za-z])[A-Za-z!0-9@#*]{2,10}$/;
const EMAIL_PATTERN = /^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$/;

export const fullNameRegex = (value) =>
    typeof value === "string" && FULL_NAME_PATTERN.test(value.trim());

export const passwordRegex = (value) =>
    typeof value === "string" && PASSWORD_PATTERN.test(value.trim());

export const emailRegex = (value) =>
    typeof value === "string" && EMAIL_PATTERN.test(value.trim());
