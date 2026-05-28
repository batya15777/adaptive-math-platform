
import './App.css'
import RegisterForm from "./components/RegisterForm.jsx";
import {ProfileSettings} from "./components/ProfileSettings/ProfileSettings.jsx";
import {ProfileProvider} from "./contexts/ProfileContext.jsx";

const App = () => {
    return (
        <ProfileProvider>
            {/* Other components like your Nav or Sidebar can go here */}
            <ProfileSettings />
        </ProfileProvider>
    );
};

export default App
