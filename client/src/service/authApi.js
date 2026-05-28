import api from "./api";

export const register = (data)=>{
    return api.post("/auth/register", data)
}
export const verifyCode = (data) => {
    return api.post("/auth/verify",  data);
}
