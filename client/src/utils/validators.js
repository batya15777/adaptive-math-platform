export const usernameRegex = (value) => {
    const checkUsername = /^[A-Za-z]{2,10}$/;
    return checkUsername.test(value.trim());
}

export const passwordRegex = (value) => {
    const checkPassword = /^[A-Za-z!0-9@#*]{2,10}$/;
    return checkPassword.test(value.trim());
}

export const emailRegex = (value) => {
    const checkEmail = /^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$/;
    return checkEmail.test(value.trim());
}