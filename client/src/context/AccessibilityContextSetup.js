import { createContext, useContext } from 'react';
import { A11Y_DEFAULTS } from '../utils/accessibility.js';

export const AccessibilityContext = createContext({ settings: A11Y_DEFAULTS, update: () => {} });

export const useAccessibility = () => useContext(AccessibilityContext);
