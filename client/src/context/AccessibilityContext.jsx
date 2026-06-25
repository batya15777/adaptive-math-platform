import { useState, useEffect } from 'react';
import { AccessibilityContext } from './AccessibilityContextSetup.js';
import { loadA11y, saveA11y, applyA11y } from '../utils/accessibility.js';

// Holds the accessibility preferences and applies them to <html> for the whole app. Client-only:
// state lives in localStorage, never on the server.
export const AccessibilityProvider = ({ children }) => {
    const [settings, setSettings] = useState(loadA11y);

    useEffect(() => { applyA11y(settings); saveA11y(settings); }, [settings]);

    const update = (patch) => setSettings((s) => ({ ...s, ...patch }));

    return (
        <AccessibilityContext.Provider value={{ settings, update }}>
            {children}
        </AccessibilityContext.Provider>
    );
};
