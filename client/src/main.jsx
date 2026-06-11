import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import './index.css'
import App from './App.jsx'
import { AuthProvider } from './context/AuthContext.jsx'
import { ProfileProvider } from './contexts/ProfileContext.jsx'

createRoot(document.getElementById('root')).render(
    <StrictMode>
        <AuthProvider>
            <ProfileProvider>
                <App />
            </ProfileProvider>
        </AuthProvider>
    </StrictMode>,
)
