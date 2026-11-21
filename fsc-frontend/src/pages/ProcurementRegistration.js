import { registerProcurementCompany } from '../api/procurementCompany.api.js';

export function renderProcurementRegistrationPage(onSignIn, onBack) {
    document.querySelector('#app').innerHTML = `
        <div class="login-container supplier-registration-container">
            <div class="login-card supplier-registration-card">

                <div class="brand">
                    <div class="brand-logo">P</div>
                    <h1>ProcureAxis</h1>
                    <p>ENTERPRISE PROCUREMENT PLATFORM</p>
                </div>

                <div class="welcome-section">
                    <h2>Register Your Procurement Company</h2>
                    <p>Create your company account to access the ProcureAxis Procurement Portal.</p>
                </div>

                <form id="procurement-registration-form">

                    <h3 class="registration-section-title">Company Information</h3>

                    <div class="form-group">
                        <label for="procurementCompanyName">Company Name *</label>
                        <input
                            type="text"
                            id="procurementCompanyName"
                            name="companyName"
                            placeholder="Enter company name"
                            required
                        >
                    </div>

                    <div class="form-group">
                        <label for="procurementLegalName">Legal Name *</label>
                        <input
                            type="text"
                            id="procurementLegalName"
                            name="legalName"
                            placeholder="Enter registered legal name"
                            required
                        >
                    </div>

                    <div class="profile-grid">
                        <div class="form-group">
                            <label for="procurementGstNumber">GST Number</label>
                            <input
                                type="text"
                                id="procurementGstNumber"
                                name="gstNumber"
                                placeholder="Enter GST number"
                            >
                        </div>

                        <div class="form-group">
                            <label for="procurementPanNumber">PAN Number</label>
                            <input
                                type="text"
                                id="procurementPanNumber"
                                name="panNumber"
                                placeholder="Enter PAN number"
                            >
                        </div>
                    </div>

                    <div class="form-group">
                        <label for="procurementEmailAddress">Company Email Address *</label>
                        <input
                            type="email"
                            id="procurementEmailAddress"
                            name="emailAddress"
                            placeholder="Enter company email"
                            required
                        >
                    </div>

                    <div class="profile-grid">
                        <div class="form-group">
                            <label for="procurementContactNumber">Contact Number</label>
                            <input
                                type="tel"
                                id="procurementContactNumber"
                                name="contactNumber"
                                placeholder="Enter contact number"
                            >
                        </div>

                        <div class="form-group">
                            <label for="procurementWebsite">Website</label>
                            <input
                                type="url"
                                id="procurementWebsite"
                                name="website"
                                placeholder="https://example.com"
                            >
                        </div>
                    </div>

                    <h3 class="registration-section-title">Procurement Settings</h3>

                    <div class="profile-grid">
                        <div class="form-group">
                            <label for="fiscalYearStartMonth">Fiscal Year Start Month</label>
                            <select id="fiscalYearStartMonth" name="fiscalYearStartMonth">
                                <option value="">Select month (optional)</option>
                                <option value="1">January</option>
                                <option value="2">February</option>
                                <option value="3">March</option>
                                <option value="4">April</option>
                                <option value="5">May</option>
                                <option value="6">June</option>
                                <option value="7">July</option>
                                <option value="8">August</option>
                                <option value="9">September</option>
                                <option value="10">October</option>
                                <option value="11">November</option>
                                <option value="12">December</option>
                            </select>
                        </div>

                        <div class="form-group">
                            <label for="approvalRequired">Require Approval?</label>
                            <select id="approvalRequired" name="approvalRequired">
                                <option value="">Use default setting</option>
                                <option value="true">Yes</option>
                                <option value="false">No</option>
                            </select>
                        </div>
                    </div>

                    <button type="submit" id="procurement-register-button">
                        Register Company
                    </button>

                    <p id="procurement-registration-message" role="status"></p>

                </form>

                <div class="supplier-registration-footer">
                    <p>Already registered?
                        <a href="#" id="procurement-signin-link">Sign In</a>
                    </p>
                </div>

                <button
                    type="button"
                    class="back-button"
                    id="procurement-registration-back">
                    Back to Portal Selection
                </button>

            </div>
        </div>
    `;

    const form = document.querySelector('#procurement-registration-form');
    const message = document.querySelector('#procurement-registration-message');
    const button = document.querySelector('#procurement-register-button');

    document.querySelector('#procurement-signin-link')
        .addEventListener('click', event => {
            event.preventDefault();
            if (typeof onSignIn === 'function') onSignIn();
        });

    document.querySelector('#procurement-registration-back')
        .addEventListener('click', () => {
            if (typeof onBack === 'function') onBack();
        });

    form.addEventListener('submit', async event => {
        event.preventDefault();

        if (!form.reportValidity()) return;

        const formData = new FormData(form);

        const fiscalYear = formData.get('fiscalYearStartMonth');
        const approval = formData.get('approvalRequired');

        const registrationData = {
            companyName: formData.get('companyName').trim(),
            legalName: formData.get('legalName').trim(),
            gstNumber: formData.get('gstNumber').trim() || null,
            panNumber: formData.get('panNumber').trim() || null,
            emailAddress: formData.get('emailAddress').trim(),
            contactNumber: formData.get('contactNumber').trim() || null,
            website: formData.get('website').trim() || null,
            fiscalYearStartMonth: fiscalYear ? Number(fiscalYear) : null,
            approvalRequired: approval === '' ? null : approval === 'true'
        };

        button.disabled = true;
        button.textContent = 'Registering...';
        message.textContent = '';
        message.className = '';

        try {
            const response = await registerProcurementCompany(registrationData);

            console.log('Procurement Company Registration Response:', response);

            message.innerHTML = `
                <p>Company registered successfully!</p>
                <p>Please check your email for the activation token.</p>
                <button type="button" id="go-to-procurement-activation">
                    Activate Your Account
                </button>
            `;

            message.className = 'success-message';
            form.reset();

            document.querySelector('#go-to-procurement-activation')
                .addEventListener('click', () => {
                    window.location.href = '/?portal=procurement-activation';
                });

        } catch (error) {
            console.error('Procurement Company Registration Failed:', error);

            message.textContent =
                error.response?.data?.message ||
                error.response?.data?.detail ||
                error.response?.data ||
                'Unable to register procurement company. Please try again.';

            message.className = 'error-message';

        } finally {
            button.disabled = false;
            button.textContent = 'Register Company';
        }
    });
}