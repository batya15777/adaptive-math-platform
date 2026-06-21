import {useState} from "react";
import { useNavigate } from "react-router-dom";
import {register, verifyCode} from "../service/authApi.js";
import { useLanguage } from "../i18n/useLanguage.js";
import { getAuthStrings } from "./authStrings.js";
import { LanguageSwitcher } from "./LanguageSwitcher/LanguageSwitcher.jsx";
function RegisterForm(){

    const { language, dir } = useLanguage();
    const t = getAuthStrings(language);

    const [fullName, setFullName] = useState("");
    const [password, setPassword] = useState("");
    const [confirmPassword, setConfirmPassword] = useState("");
    const [email, setEmail] = useState("");
    const [age, setAge] = useState("");
    const [gender, setGender] = useState("");
    const [code, setCode] = useState("");
    const [showCode, setShowCode] = useState(false);
    const [showPassword, setShowPassword] = useState(false);
    const [showConfirmPassword, setShowConfirmPassword] = useState(false);
    const [errors, setErrors] = useState("");

    const navigate = useNavigate();


    const fullNameRegex =(value)=>{
        const checkFullName = /^[A-Za-z]+(?: [A-Za-z]+)+$/;
        return checkFullName.test(value.trim());
    }
    const passwordRegex =(value)=>{
        const checkPassword = /^[A-Za-z!0-9@#*]{2,10}$/;
        return checkPassword.test(value.trim());
    }
    const emailRegex =(value)=>{
        const checkEmail= /^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$/;
        return checkEmail.test(value.trim());
    }




    const validation =()=>{
        let hasErrors = false;
        if(!fullNameRegex(fullName)){
            hasErrors = true;
        }
        if (!passwordRegex(password)){
            hasErrors = true;
        }
        if (password !== confirmPassword){
            hasErrors = true;
        }
        if (!emailRegex(email)){
            hasErrors = true;
        }
        if (age === "" || Number(age) < 1 || Number(age) > 120){
            hasErrors = true;
        }
        if (gender.trim().length === 0){
            hasErrors = true;
        }
        return hasErrors;

    }
 const handleRegister =(e)=>{
        e.preventDefault();
        setErrors("")
        if(validation())return;

   const data = {
       fullName,password,email,age: Number(age),gender
   }

     register(data)
         .then(response=>{
             if (response.data.success){
                 setShowCode(true);
             }


         })
         .catch(error=>{
             console.log(error);
             setErrors(t.registerFailed);
         })

 }
    const handleVerify =()=>{
        const data = {
            code,email
        }
        verifyCode(data).then((response)=>{
            if (response.data.success){
                setShowCode(false);
                setErrors(t.registerOk)
                navigate("/login");
            }
        })
            .catch(error=>{
                setErrors(t.invalidCode);
                console.log(error);
            })


    }










    return(
     <>
         <form onSubmit={handleRegister} dir={dir}>

             <div style={{ textAlign: "end", marginBottom: 8 }}><LanguageSwitcher /></div>

             <input
                 type="text"
                 value={fullName}
                 placeholder={t.fullNamePh}
                 onChange={(e) => setFullName(e.target.value)}
             />

             <div>
                 <input
                     type={showPassword ? "text" : "password"}
                     value={password}
                     placeholder={t.passwordPh}
                     onChange={(e) => setPassword(e.target.value)}
                 />
                 <button
                     type="button"
                     onClick={() => setShowPassword((currentValue) => !currentValue)}
                 >
                     {showPassword ? t.hide : t.show}
                 </button>
             </div>

             <div>
                 <input
                     type={showConfirmPassword ? "text" : "password"}
                     value={confirmPassword}
                     placeholder={t.confirmPh}
                     onChange={(e) => setConfirmPassword(e.target.value)}
                 />
                 <button
                     type="button"
                     onClick={() => setShowConfirmPassword((currentValue) => !currentValue)}
                 >
                     {showConfirmPassword ? t.hide : t.show}
                 </button>
             </div>

             <input
                 type="email"
                 value={email}
                 placeholder={t.emailPh}
                 onChange={(e) => setEmail(e.target.value)}
             />

             <input
                 type="number"
                 value={age}
                 min="1"
                 max="120"
                 placeholder={t.agePh}
                 onChange={(e) => setAge(e.target.value)}
             />

             <input
                 type="radio"
                 name="gender"
                 value="male"
                 checked={gender === "male"}
                 onChange={(e) => setGender(e.target.value)}
             />

             <input
                 type="radio"
                 name="gender"
                 value="female"
                 checked={gender === "female"}
                 onChange={(e) => setGender(e.target.value)}
             />
             <button
                 disabled={validation()}
                 type="submit">

                 {t.register}
             </button>


             {
                 errors &&
                 <p>{errors}</p>
             }


         </form>
         {
             showCode && <>
                 <input
                     type="text"
                     value={code}
                     placeholder={t.codePh}
                     onChange={(e) => setCode(e.target.value)}
                 />
                 <button
                     onClick={handleVerify}
                     disabled={code.trim().length ===0}>{t.send}


                 </button>

             </>
         }
     </>
    )
}
export default RegisterForm;
