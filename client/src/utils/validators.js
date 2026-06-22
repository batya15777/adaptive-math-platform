const FULL_NAME_PATTERN = /^[A-Za-z]+(?: [A-Za-z]+)+$/;
const PASSWORD_PATTERN = /^[A-Za-z!0-9@#*]{2,10}$/;
const EMAIL_PATTERN = /^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$/;

export const fullNameRegex = (value) =>
    typeof value === "string" && FULL_NAME_PATTERN.test(value.trim());

export const passwordRegex = (value) =>
    typeof value === "string" && PASSWORD_PATTERN.test(value.trim());

export const emailRegex = (value) =>
    typeof value === "string" && EMAIL_PATTERN.test(value.trim());
