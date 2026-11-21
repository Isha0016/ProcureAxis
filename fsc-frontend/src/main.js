import './style.css';

import { loginEmployee } from './api/auth.api.js';
import { renderSupplierDashboard } from './pages/supplier.js';
import { renderProcurementDashboard } from './pages/procurement.js';
import { renderApprovalManagerDashboard } from './pages/approvalManager.js';
import { renderInternalAdminDashboard } from "./pages/internalAdmin.js";
import { renderSupplierRegistrationPage }from './pages/SupplierRegistration.js';
import { renderSupplierActivationPage} from "./pages/supplierActivation.js";
import { renderProcurementRegistrationPage} from "./pages/ProcurementRegistration.js";
import { renderProcurementActivationPage} from "./pages/procurementActivation.js";

const app = document.querySelector('#app');

// -----------------------------
// LANDING PAGE
// -----------------------------

function renderLandingPage() {
    app.innerHTML = `
        <div class="landing-container">

            <header class="landing-header">
                <div class="landing-brand">
                    <div class="brand-logo">P</div>
                    <div>
                        <h1>ProcureAxis</h1>
                        <p>ENTERPRISE PROCUREMENT PLATFORM</p>
                    </div>
                </div>
            </header>

            <section class="landing-hero">
                <span class="hero-tag">SMART PROCUREMENT SOLUTIONS</span>

                <h1>
                    Simplifying Procurement.<br>
                    Connecting Businesses.
                </h1>

                <p>
                    ProcureAxis brings procurement companies and suppliers
                    together through one centralized platform.
                    Manage purchasing, products, sourcing and bids efficiently.
                </p>
            </section>

            <section class="portal-section">
                <h2>Choose Your Portal</h2>
                <p>Select your account type to continue.</p>

                <div class="portal-cards">

                    <div class="portal-card">
                        <div class="portal-icon">🚚</div>
                        <h3>Supplier Portal</h3>
                        <p>
                            Manage products, explore sourcing opportunities,
                            submit bids and maintain your company profile.
                        </p>
                        <button id="supplierPortalButton">
                            Enter Supplier Portal →
                        </button>
                    </div>

                    <div class="portal-card">
                        <div class="portal-icon">🏢</div>
                        <h3>Procurement Portal</h3>
                        <p>
                            Manage purchase requests, sourcing activities,
                            suppliers, bids and procurement approvals.
                        </p>
                        <button id="procurementPortalButton">
                            Enter Procurement Portal →
                        </button>
                    </div>

                    <div class="portal-card internal-admin-card">
                        <div class="portal-icon">🛡️</div>
                        <h3>Internal Admin Portal</h3>
                        <p>
                            Manage supplier and procurement company approvals,
                            administration and user management.
                        </p>
                        <button id="internalAdminPortalButton">
                            Enter Internal Admin Portal →
                        </button>
                    </div>

                </div>
            </section>

            <footer class="landing-footer">
                Intelligent Procurement. Connected Supply Chains.
            </footer>

        </div>
    `;

    document.querySelector('#supplierPortalButton')
        .addEventListener('click', showSupplierRegistration);

    document.querySelector('#procurementPortalButton')
        .addEventListener('click', renderProcurementAccessPage);

    document.querySelector('#internalAdminPortalButton')
        .addEventListener('click', () => renderLoginPage('internal-admin'));
}

// -----------------------------
// PROCUREMENT ROLE SELECTION
// -----------------------------

function renderProcurementAccessPage() {
    app.innerHTML = `
        <div class="landing-container">

            <header class="landing-header">
                <div class="landing-brand">
                    <div class="brand-logo">P</div>
                    <div>
                        <h1>ProcureAxis</h1>
                        <p>ENTERPRISE PROCUREMENT PLATFORM</p>
                    </div>
                </div>
            </header>

            <section class="portal-section procurement-access-section">

                <h2>Choose Your Procurement Access</h2>
                <p>Select your role to continue.</p>

                <div class="portal-cards">

                    <div class="portal-card">
                        <div class="portal-icon">🏢</div>
                        <h3>Procurement User</h3>
                        <p>
                            Manage purchase requests, sourcing,
                            suppliers, bids and procurement activities.
                        </p>
                        <button id="procurementUserButton">
                            Continue as Procurement User →
                        </button>
                    </div>

                    <div class="portal-card">
                        <div class="portal-icon">📋</div>
                        <h3>Approval Manager</h3>
                        <p>
                            Review purchase requests,
                            manage approvals and track decisions.
                        </p>
                        <button id="approvalManagerButton">
                            Continue as Approval Manager →
                        </button>
                    </div>

                </div>

                <button id="backToLanding" class="back-button">
                    ← Back to Main Page
                </button>

            </section>
        </div>
    `;

    document.querySelector('#procurementUserButton')
        .addEventListener('click', showProcurementRegistration);

    document.querySelector('#approvalManagerButton')
        .addEventListener('click', () => renderLoginPage('approval-manager'));

    document.querySelector('#backToLanding')
        .addEventListener('click', renderLandingPage);
}

// -----------------------------
// LOGIN PAGE
// -----------------------------

function renderLoginPage(portal , returnToRegistration=false) {

    const isSupplier = portal === 'supplier';
    const isApprovalManager = portal === 'approval-manager';
    const isInternalAdmin = portal === 'internal-admin';

    const portalTitle = isSupplier
        ? 'Supplier Login'
        : isApprovalManager
            ? 'Approval Manager Login'
            : isInternalAdmin
                ? 'Internal Admin Login'
                : 'Procurement User Login';

    const portalDescription = isSupplier
        ? 'Sign in to access your supplier workspace.'
        : isApprovalManager
            ? 'Sign in to access your approval workspace.'
            : isInternalAdmin
                ? 'Sign in to access internal administration.'
                : 'Sign in to access your procurement workspace.';

    app.innerHTML = `
        <div class="login-container">
            <div class="login-card">

                <div class="brand">
                    <div class="brand-logo">P</div>
                    <h1>ProcureAxis</h1>
                    <p>ENTERPRISE PROCUREMENT PLATFORM</p>
                </div>

                <div class="welcome-section">
                    <h2>${portalTitle}</h2>
                    <p>${portalDescription}</p>
                </div>

                <form id="loginForm">

                    <div class="form-group">
                        <label for="email">Business Email</label>
                        <input
                            type="email"
                            id="email"
                            placeholder="name@company.com"
                            required
                        />
                    </div>

                    <div class="form-group">
                        <label for="password">Password</label>
                        <input
                            type="password"
                            id="password"
                            placeholder="Enter your password"
                            required
                        />
                    </div>

                    <button type="submit" id="loginButton">
                        Sign In
                    </button>

                    <p id="message"></p>
                </form>

                <button type="button" id="backButton" class="back-button">
                    ← Back to Portal Selection
                </button>

                <div class="login-footer">
                    Intelligent Procurement. Connected Supply Chains.
                </div>

            </div>
        </div>
    `;


    const loginForm = document.querySelector('#loginForm');

    document.querySelector('#backButton')
        .addEventListener('click', () => {
            if (portal === 'supplier' && returnToRegistration) {
                showSupplierRegistration();
            } else {
                renderLandingPage();
            }
        });

    const message = document.querySelector('#message');
    const loginButton = document.querySelector('#loginButton');

    // -----------------------------
    // LOGIN AUTHENTICATION
    // -----------------------------

    loginForm.addEventListener('submit', async (event) => {

        event.preventDefault();

        const email = document.querySelector('#email').value.trim();
        const password = document.querySelector('#password').value;

        try {

            loginButton.disabled = true;
            loginButton.textContent = 'Signing in...';
            message.textContent = '';

            await loginEmployee(email, password);

            const token = localStorage.getItem('token');

            if (!token) {
                throw new Error('Authentication token not found.');
            }

            // Decode JWT payload
            const base64 = token.split('.')[1]
                .replace(/-/g, '+')
                .replace(/_/g, '/');

            const paddedBase64 = base64.padEnd(
                base64.length + (4 - base64.length % 4) % 4,
                '='
            );

            const payload = JSON.parse(atob(paddedBase64));

            const roles = Array.isArray(payload.roles)
                ? payload.roles
                : [payload.roles || payload.role || ''];

            console.log('User roles:', roles);

            const normalizedRoles = roles.map(role =>
                String(role).toLowerCase()
            );

            const hasSupplierRole = normalizedRoles.some(role =>
                role.includes('supplier')
            );

            const hasProcurementRole = normalizedRoles.some(role =>
                role.includes('procurement') ||
                role.includes('buyer') ||
                role.includes('purchaser')
            );

            const hasApprovalManagerRole = normalizedRoles.some(role =>
                role.includes('approval') ||
                role.includes('manager')
            );

            const hasInternalAdminRole = normalizedRoles.some(role =>
                role.includes('internal') || role.includes('admin')
            );

            // Supplier Portal
            if (portal === 'supplier') {

                if (hasSupplierRole) {
                    renderSupplierDashboard();
                } else {
                    throw new Error(
                        'This account does not have Supplier access.'
                    );
                }

            }

            // Procurement User Portal
            else if (portal === 'procurement-user') {

                if (hasProcurementRole) {
                    renderProcurementDashboard();
                } else {
                    throw new Error(
                        'This account does not have Procurement access.'
                    );
                }

            }

            // Approval Manager Portal
            else if (portal === 'approval-manager') {

                if (hasApprovalManagerRole) {
                    renderApprovalManagerDashboard();
                } else {
                    throw new Error(
                        'This account does not have Approval Manager access. Check the assigned role.'
                    );
                }

            }

            // Internal Admin
            else if (portal === 'internal-admin') {

                if (hasInternalAdminRole) {
                    renderInternalAdminDashboard();
                } else {
                    throw new Error(
                        'This account does not have Internal Admin access.'
                    );
                }

            }

        } catch (error) {

            message.style.color = 'red';

            message.textContent =
                error.response?.data?.message ||
                error.message ||
                'Login failed.';

            console.error('Login error:', error);

        } finally {

            loginButton.disabled = false;
            loginButton.textContent = 'Sign In';

        }

    });
}

function showSupplierRegistration() {
    renderSupplierRegistrationPage(
        () => {
            renderLoginPage('supplier');
        },
        () => {
            renderLandingPage();
        }
    );
}

function showProcurementRegistration() {
    renderProcurementRegistrationPage(
        () => renderLoginPage('procurement-user'),
        () => renderLandingPage()
    );
}

function showSupplierActivation() {
    renderSupplierActivationPage(
        () => renderLoginPage('supplier')
    );
}
function showProcurementActivation() {
    renderProcurementActivationPage(
        () => renderLoginPage('procurement-user')
    );
}


// -----------------------------
// INITIAL PAGE
// -----------------------------

function showActivationChoice(token) {
    document.querySelector('#app').innerHTML = `
        <div class="login-container supplier-registration-container">
            <div class="login-card supplier-registration-card">

                <div class="brand">
                    <div class="brand-logo">P</div>
                    <h1>ProcureAxis</h1>
                    <p>ENTERPRISE PROCUREMENT PLATFORM</p>
                </div>

                <div class="welcome-section">
                    <h2>Activate Your Account</h2>
                    <p>Select the portal where you registered your company.</p>
                </div>

                <button id="supplier-activation-choice">
                    Activate Supplier Account
                </button>

                <button id="procurement-activation-choice">
                    Activate Procurement Account
                </button>

            </div>
        </div>
    `;

    document.querySelector('#supplier-activation-choice')
        .addEventListener('click', () => {
            showSupplierActivation();
            const tokenInput = document.querySelector('#activationToken');
            if (tokenInput) tokenInput.value = token;
        });

    document.querySelector('#procurement-activation-choice')
        .addEventListener('click', () => {
            showProcurementActivation();
            const tokenInput = document.querySelector('#procurementActivationToken');
            if (tokenInput) tokenInput.value = token;
        });
}

const urlParams = new URLSearchParams(window.location.search);
const activationToken = urlParams.get('token');
const portal = urlParams.get('portal');

if (portal === 'supplier-activation') {
    showSupplierActivation();

} else if (portal === 'procurement-activation') {
    showProcurementActivation();

} else if (activationToken) {
    showActivationChoice(activationToken);

} else {
    renderLandingPage();
}