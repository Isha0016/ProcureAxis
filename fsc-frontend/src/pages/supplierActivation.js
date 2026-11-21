import { activateEmployee } from '../api/employee.api.js';

export function renderSupplierActivationPage(onSignIn) {
    document.querySelector('#app').innerHTML = `
        <div class="login-container supplier-registration-container">
            <div class="login-card supplier-registration-card">

                <div class="brand">
                    <div class="brand-logo">P</div>
                    <h1>ProcureAxis</h1>
                    <p>ENTERPRISE PROCUREMENT PLATFORM</p>
                </div>

                <div class="welcome-section">
                    <h2>Activate Your Supplier Account</h2>
                    <p>Enter the activation token received in your email and set your password.</p>
                </div>

                <form id="supplier-activation-form">

                    <div class="form-group">
                        <label for="activationToken">Activation Token *</label>
                        <input type="text" id="activationToken"
                               name="activationToken" required
                               placeholder="Paste your activation token">
                    </div>

                    <div class="form-group">
                        <label for="newPassword">New Password *</label>
                        <input type="password" id="newPassword"
                               name="newPassword" required minlength="8">
                    </div>

                    <div class="form-group">
                        <label for="confirmPassword">Confirm Password *</label>
                        <input type="password" id="confirmPassword"
                               name="confirmPassword" required minlength="8">
                    </div>

                    <button type="submit" id="supplier-activate-button">
                        Activate Account
                    </button>

                    <p id="supplier-activation-message" role="status"></p>
                </form>

                <div class="supplier-registration-footer">
                    Already activated?
                    <a href="#" id="supplier-activation-signin">Sign In</a>
                </div>

            </div>
        </div>
    `;

    const form = document.querySelector('#supplier-activation-form');
    const message = document.querySelector('#supplier-activation-message');
    const submitButton = document.querySelector('#supplier-activate-button');

    // Prefill token if it is present in the URL.
    const urlToken = new URLSearchParams(window.location.search).get('token');
    if (urlToken) {
        form.elements.activationToken.value = urlToken;
    }

    form.addEventListener('submit', async (event) => {
        event.preventDefault();

        if (!form.reportValidity()) return;

        const token = form.elements.activationToken.value.trim();
        const newPassword = form.elements.newPassword.value;
        const confirmPassword = form.elements.confirmPassword.value;

        if (newPassword !== confirmPassword) {
            message.textContent = 'Passwords do not match.';
            message.className = 'error-message';
            return;
        }

        submitButton.disabled = true;
        submitButton.textContent = 'Activating...';
        message.textContent = '';
        message.className = '';

        try {
            await activateEmployee(token, newPassword);

            message.textContent =
                'Your account has been activated successfully. You can now sign in.';
            message.className = 'success-message';

            form.reset();

            setTimeout(() => {
                onSignIn();
            }, 1500);

        } catch (error) {
            message.textContent =
                error.response?.data?.message ||
                error.response?.data?.error ||
                error.message ||
                'Account activation failed. Please try again.';

            message.className = 'error-message';

        } finally {
            submitButton.disabled = false;
            submitButton.textContent = 'Activate Account';
        }
    });

    document.querySelector('#supplier-activation-signin')
        .addEventListener('click', (event) => {
            event.preventDefault();
            onSignIn();
        });
}