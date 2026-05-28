import {useState} from "react";
import {register, verifyCode} from "../service/authApi.js";

function RegisterForm(){

    const [username, setUsername] = useState("");
    const [password, setPassword] = useState("");
    const [email, setEmail] = useState("");
    const [gender, setGender] = useState("");
    const [code, setCode] = useState("");
    const [showCode, setShowCode] = useState(false);
    const [errors, setErrors] = useState("");


    const usernameRegex =(value)=>{
        const checkUsername = /^[A-Za-z]{2,10}$/;
        return checkUsername.test(value.trim());
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
        if(!usernameRegex(username)){
            hasErrors = true;
        }
        if (!passwordRegex(password)){
            hasErrors = true;
        }
        if (!emailRegex(email)){
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
       username,password,email,gender
   }

     register(data)
         .then(response=>{
             if (response.data.success){
                 setShowCode(true);
             }


         })
         .catch(error=>{
             console.log(error);
             setErrors("Registration failed");
         })

 }
    const handleVerify =()=>{
        const data = {
            code,email
        }
        verifyCode(data).then((response)=>{
            if (response.data.success){
                setShowCode(false);
                setErrors("You have registered successfully.")
            }
        })
            .catch(error=>{
                setErrors("Invalid code");
                console.log(error);
            })


    }










    return(
     <>
         <form onSubmit={handleRegister} >

             <input
                 type="text"
                 value={username}
                 placeholder="Enter username"
                 onChange={(e) => setUsername(e.target.value)}
             />

             <input
                 type="password"
                 value={password}
                 placeholder="Enter password"
                 onChange={(e) => setPassword(e.target.value)}
             />

             <input
                 type="email"
                 value={email}
                 placeholder="Enter email"
                 onChange={(e) => setEmail(e.target.value)}
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

                 Register
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
                     placeholder="Enter Code"
                     onChange={(e) => setCode(e.target.value)}
                 />
                 <button
                     onClick={handleVerify}
                     disabled={code.trim().length ===0}>send


                 </button>
             </>
         }
     </>
    )
}
export default RegisterForm;