import { registerSupplierCompany } from '../api/supplierCompany.api.js';
import { activateEmployee } from '../api/employee.api.js';

export function renderSupplierRegistrationPage(onSignIn, onBack) {
    document.querySelector('#app').innerHTML = `
        <div class="login-container supplier-registration-container">
            <div class="login-card supplier-registration-card">

                <div class="brand">
                    <div class="brand-logo">P</div>
                    <h1>ProcureAxis</h1>
                    <p>ENTERPRISE PROCUREMENT PLATFORM</p>
                </div>

                <div class="welcome-section">
                    <h2>Register Your Supplier Company</h2>
                    <p>Submit your company details to join our supplier network.</p>
                </div>

                <!-- SUPPLIER REGISTRATION -->
                <form id="supplier-registration-form">

                    <h3 class="registration-section-title">Supplier Details</h3>

                    <div class="form-group">
                        <label for="supplierCategory">Supplier Category *</label>
                        <input
                            type="text"
                            id="supplierCategory"
                            name="supplierCategory"
                            placeholder="e.g. Raw Materials, IT Services"
                            required
                        >
                    </div>

                    <div class="profile-grid">
                        <div class="form-group">
                            <label for="leadTimeDays">Lead Time (Days)</label>
                            <input
                                type="number"
                                id="leadTimeDays"
                                name="leadTimeDays"
                                min="1"
                                placeholder="e.g. 7"
                            >
                        </div>

                        <div class="form-group">
                            <label for="minimumOrderValue">Minimum Order Value</label>
                            <input
                                type="number"
                                id="minimumOrderValue"
                                name="minimumOrderValue"
                                min="0.01"
                                step="0.01"
                                placeholder="e.g. 5000"
                            >
                        </div>
                    </div>

                    <h3 class="registration-section-title">Company Details</h3>

                    <div class="form-group">
                        <label for="companyName">Company Name *</label>
                        <input
                            type="text"
                            id="companyName"
                            name="companyName"
                            required
                        >
                    </div>

                    <div class="form-group">
                        <label for="legalName">Legal Name *</label>
                        <input
                            type="text"
                            id="legalName"
                            name="legalName"
                            required
                        >
                    </div>

                    <div class="profile-grid">
                        <div class="form-group">
                            <label for="gstNumber">GST Number</label>
                            <input
                                type="text"
                                id="gstNumber"
                                name="gstNumber"
                            >
                        </div>

                        <div class="form-group">
                            <label for="panNumber">PAN Number</label>
                            <input
                                type="text"
                                id="panNumber"
                                name="panNumber"
                            >
                        </div>
                    </div>

                    <div class="form-group">
                        <label for="emailAddress">Email Address *</label>
                        <input
                            type="email"
                            id="emailAddress"
                            name="emailAddress"
                            required
                        >
                    </div>

                    <div class="profile-grid">
                        <div class="form-group">
                            <label for="contactNumber">Contact Number</label>
                            <input
                                type="tel"
                                id="contactNumber"
                                name="contactNumber"
                            >
                        </div>

                        <div class="form-group">
                            <label for="website">Website</label>
                            <input
                                type="url"
                                id="website"
                                name="website"
                                placeholder="https://example.com"
                            >
                        </div>
                    </div>

                    <button type="submit" id="supplier-register-button">
                        Register Supplier Company
                    </button>

                    <p id="supplier-registration-message" role="status"></p>
                </form>

                <!-- SUPPLIER ACCOUNT ACTIVATION -->
                <div class="activation-section">

                    <h3 class="registration-section-title">
                        Activate Your Account
                    </h3>

                    <p>
                        Enter the activation token received in your email
                        and set your password.
                    </p>

                    <form id="supplier-activation-form">

                        <div class="form-group">
                            <label for="supplierActivationToken">
                                Activation Token *
                            </label>
                            <input
                                type="text"
                                id="supplierActivationToken"
                                name="activationToken"
                                required
                                placeholder="Paste your activation token"
                            >
                        </div>

                        <div class="form-group">
                            <label for="supplierNewPassword">
                                New Password *
                            </label>
                            <input
                                type="password"
                                id="supplierNewPassword"
                                name="newPassword"
                                required
                                minlength="8"
                                placeholder="Enter new password"
                            >
                        </div>

                        <div class="form-group">
                            <label for="supplierConfirmPassword">
                                Confirm Password *
                            </label>
                            <input
                                type="password"
                                id="supplierConfirmPassword"
                                name="confirmPassword"
                                required
                                minlength="8"
                                placeholder="Confirm new password"
                            >
                        </div>

                        <button type="submit" id="supplier-activate-button">
                            Activate Account
                        </button>

                        <p id="supplier-activation-message" role="status"></p>

                    </form>
                </div>

                <div class="supplier-registration-footer">
                    <p>Already registered?
                        <a href="#" id="supplier-signin-link">Sign In</a>
                    </p>
                </div>

                <button type="button" class="back-button" id="supplier-registration-back">
                    Back to Portal Selection
                </button>

            </div>
        </div>
    `;

    const form = document.querySelector('#supplier-registration-form');
    const message = document.querySelector('#supplier-registration-message');
    const submitButton = document.querySelector('#supplier-register-button');

    // SUPPLIER REGISTRATION HANDLER
    form.addEventListener('submit', async (event) => {
        event.preventDefault();

        if (!form.reportValidity()) return;

        const formData = new FormData(form);

        const registrationData = {
            supplierCategory: formData.get('supplierCategory').trim(),
            companyName: formData.get('companyName').trim(),
            legalName: formData.get('legalName').trim(),
            emailAddress: formData.get('emailAddress').trim(),
            gstNumber: formData.get('gstNumber').trim(),
            panNumber: formData.get('panNumber').trim(),
            website: formData.get('website').trim(),
            contactNumber: formData.get('contactNumber').trim()
        };

        const leadTime = formData.get('leadTimeDays');
        const minimumOrderValue = formData.get('minimumOrderValue');

        if (leadTime) {
            registrationData.leadTimeDays = Number(leadTime);
        }

        if (minimumOrderValue) {
            registrationData.minimumOrderValue = Number(minimumOrderValue);
        }

        submitButton.disabled = true;
        submitButton.textContent = 'Registering...';
        message.textContent = '';
        message.className = '';

        try {
            await registerSupplierCompany(registrationData);

            message.textContent =
                'Registration successful! Please check your email for the activation token.';

            message.className = 'success-message';

            form.reset();

        } catch (error) {
            message.textContent =
                error.response?.data?.message ||
                error.response?.data?.error ||
                error.message ||
                'Registration failed. Please try again.';

            message.className = 'error-message';

        } finally {
            submitButton.disabled = false;
            submitButton.textContent = 'Register Supplier Company';
        }
    });

    // SUPPLIER ACTIVATION HANDLER
    const activationForm =
        document.querySelector('#supplier-activation-form');

    const activationMessage =
        document.querySelector('#supplier-activation-message');

    const activationButton =
        document.querySelector('#supplier-activate-button');

    activationForm.addEventListener('submit', async (event) => {
        event.preventDefault();

        if (!activationForm.reportValidity()) return;

        const token =
            activationForm.elements.activationToken.value.trim();

        const newPassword =
            activationForm.elements.newPassword.value;

        const confirmPassword =
            activationForm.elements.confirmPassword.value;

        if (newPassword !== confirmPassword) {
            activationMessage.textContent = 'Passwords do not match.';
            activationMessage.className = 'error-message';
            return;
        }

        activationButton.disabled = true;
        activationButton.textContent = 'Activating...';

        activationMessage.textContent = '';
        activationMessage.className = '';

        try {
            await activateEmployee(token, newPassword);

            activationMessage.textContent =
                'Supplier account activated successfully! You can now sign in.';

            activationMessage.className = 'success-message';

            activationForm.reset();

        } catch (error) {
            activationMessage.textContent =
                error.response?.data?.message ||
                error.response?.data?.error ||
                error.message ||
                'Account activation failed. Please try again.';

            activationMessage.className = 'error-message';

        } finally {
            activationButton.disabled = false;
            activationButton.textContent = 'Activate Account';
        }
    });

    // SIGN IN
    document.querySelector('#supplier-signin-link')
        .addEventListener('click', (event) => {
            event.preventDefault();

            if (typeof onSignIn === 'function') {
                onSignIn();
            } else {
                console.error('Sign In callback is missing');
            }
        });

    // BACK TO PORTAL SELECTION
    document.querySelector('#supplier-registration-back')
        .addEventListener('click', () => {
            onBack();
        });
}