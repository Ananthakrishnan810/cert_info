document.addEventListener('DOMContentLoaded', () => {
    // Cluster & Certificate Modal Elements
    const clusterModal = document.getElementById('clusterModal');
    const certModal = document.getElementById('certModal');
    const openClusterModalBtn = document.getElementById('openClusterModalBtn');
    const openCertModalBtn = document.getElementById('openCertModalBtn');
    const closeClusterModalBtn = document.getElementById('closeClusterModalBtn');
    const cancelClusterModalBtn = document.getElementById('cancelClusterModalBtn');
    const closeCertModalBtn = document.getElementById('closeCertModalBtn');
    const cancelCertModalBtn = document.getElementById('cancelCertModalBtn');
    const createClusterForm = document.getElementById('createClusterForm');
    const createCertForm = document.getElementById('createCertForm');
    const clustersContainer = document.getElementById('clustersContainer');
    const certClusterSelect = document.getElementById('certClusterSelect');

    const clusterModalTitle = document.getElementById('clusterModalTitle');
    const editClusterId = document.getElementById('editClusterId');
    const saveClusterBtn = document.getElementById('saveClusterBtn');

    const certModalTitle = document.getElementById('certModalTitle');
    const editCertId = document.getElementById('editCertId');
    const editCertClusterId = document.getElementById('editCertClusterId');
    const certClusterGroup = document.getElementById('certClusterGroup');
    const certNameInput = document.getElementById('certName');
    const certIssuedDateInput = document.getElementById('certIssuedDate');
    const certEndDateInput = document.getElementById('certEndDate');

    let clustersData = [];

    // --- CLUSTERS & CERTIFICATE API LOGIC ---


    async function fetchClusters() {
        try {
            const res = await fetch('/api/clusters?t=' + Date.now());
            clustersData = await res.json();
            renderClusters(clustersData);
            populateClusterDropdown(clustersData);
        } catch (err) {
            console.error('Failed to fetch clusters:', err);
            clustersContainer.innerHTML = `<div class="card glass" style="color: #ef4444;">Failed to load cluster data from .txt storage.</div>`;
        }
    }

    function renderClusters(clusters) {
        if (!clusters || clusters.length === 0) {
            clustersContainer.innerHTML = `
                <div class="card glass" style="text-align: center; padding: 40px;">
                    <i class="fa-solid fa-folder-open" style="font-size: 3rem; color: var(--text-dim); margin-bottom: 16px;"></i>
                    <h3>No Clusters Configured</h3>
                    <p style="color: var(--text-muted); margin-top: 8px;">Click "Create New Cluster" above to add your first cluster and SSL certificates.</p>
                </div>
            `;
            return;
        }

        clustersContainer.innerHTML = clusters.map(cluster => `
            <div class="cluster-card glass">
                <div class="cluster-header">
                    <div>
                        <div class="cluster-title">
                            <i class="fa-solid fa-server" style="color: var(--accent-blue);"></i>
                            <h3>${escapeHtml(cluster.clusterName)}</h3>
                            <span class="sub-text">(${cluster.certificates ? cluster.certificates.length : 0} certs)</span>
                        </div>
                        <p style="color: var(--text-muted); font-size: 0.85rem; margin-top: 4px;">${escapeHtml(cluster.description || 'No description provided')}</p>
                    </div>
                    <div style="display: flex; align-items: center; gap: 8px;">
                        <div class="cluster-recipients">
                            <i class="fa-solid fa-envelope"></i>
                            <span>${escapeHtml(cluster.recipientEmails)}</span>
                        </div>
                        <button class="btn btn-secondary btn-edit-cluster" data-id="${cluster.id}" onclick="editCluster('${cluster.id}')" title="Edit Cluster" style="padding: 6px 10px; font-size: 0.78rem;">
                            <i class="fa-solid fa-pen-to-square"></i> Edit
                        </button>
                        <button class="btn btn-secondary btn-delete-cluster" data-id="${cluster.id}" onclick="deleteCluster('${cluster.id}')" title="Delete Cluster" style="padding: 6px 10px; font-size: 0.78rem; color: #ef4444;">
                            <i class="fa-solid fa-trash"></i> Delete
                        </button>
                    </div>
                </div>

                ${renderCertificatesTable(cluster.id, cluster.certificates)}
            </div>
        `).join('');
    }

    function renderCertificatesTable(clusterId, certificates) {
        if (!certificates || certificates.length === 0) {
            return `
                <div style="padding: 16px; text-align: center; color: var(--text-dim); font-size: 0.85rem;">
                    No certificates in this cluster. <a href="#" onclick="openAddCertForCluster('${clusterId}'); return false;" style="color: var(--accent-blue);">+ Add one now</a>
                </div>
            `;
        }

        return `
            <table class="certs-table">
                <thead>
                    <tr>
                        <th>Certificate Name</th>
                        <th>Issued Date</th>
                        <th>End / Expiry Date</th>
                        <th>Days Left</th>
                        <th>Status</th>
                        <th>Actions</th>
                    </tr>
                </thead>
                <tbody>
                    ${certificates.map(cert => {
            let statusClass = 'status-active';
            if (cert.status && cert.status.includes('EXPIRING')) statusClass = 'status-expiring';
            if (cert.status && cert.status.includes('EXPIRED')) statusClass = 'status-expired';

            return `
                            <tr>
                                <td><strong>${escapeHtml(cert.certificateName)}</strong></td>
                                <td>${escapeHtml(cert.issuedDate)}</td>
                                <td>${escapeHtml(cert.endDate)}</td>
                                <td><strong>${cert.daysRemaining} days</strong></td>
                                <td><span class="status-badge ${statusClass}">${escapeHtml(cert.status)}</span></td>
                                <td>
                                    <button class="btn btn-secondary btn-edit-cert" data-cluster-id="${clusterId}" data-cert-id="${cert.id}" onclick="editCertificate('${clusterId}', '${cert.id}')" title="Edit Certificate" style="padding: 4px 8px; font-size: 0.78rem;">
                                        <i class="fa-solid fa-pen-to-square"></i> Edit
                                    </button>
                                    <button class="btn btn-secondary btn-delete-cert" data-cluster-id="${clusterId}" data-cert-id="${cert.id}" onclick="deleteCertificate('${clusterId}', '${cert.id}')" title="Delete Certificate" style="padding: 4px 8px; font-size: 0.78rem; color: #ef4444;">
                                        <i class="fa-solid fa-trash"></i> Delete
                                    </button>
                                </td>
                            </tr>
                        `;
        }).join('')}
                </tbody>
            </table>
        `;
    }

    function populateClusterDropdown(clusters) {
        certClusterSelect.innerHTML = '<option value="">Select Target Cluster...</option>' +
            clusters.map(c => `<option value="${c.id}">${escapeHtml(c.clusterName)} (${escapeHtml(c.recipientEmails)})</option>`).join('');
    }

    function escapeHtml(str) {
        if (!str) return '';
        return str.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;').replace(/"/g, '&quot;');
    }

    // Modal Control Functions
    function openModal(modal) {
        modal.classList.add('active');
    }

    function closeModal(modal) {
        modal.classList.remove('active');
    }

    function resetClusterModal() {
        createClusterForm.reset();
        if (editClusterId) editClusterId.value = '';
        if (clusterModalTitle) clusterModalTitle.innerHTML = '<i class="fa-solid fa-folder-plus"></i> Create New Cluster';
        if (saveClusterBtn) saveClusterBtn.textContent = 'Save Cluster to .txt';
    }

    if (openClusterModalBtn) {
        openClusterModalBtn.addEventListener('click', () => {
            resetClusterModal();
            openModal(clusterModal);
        });
    }
    if (closeClusterModalBtn) closeClusterModalBtn.addEventListener('click', () => closeModal(clusterModal));
    if (cancelClusterModalBtn) cancelClusterModalBtn.addEventListener('click', () => closeModal(clusterModal));

    window.editCluster = (clusterId) => {
        const cluster = clustersData.find(c => c.id === clusterId);
        if (!cluster) return;

        if (editClusterId) editClusterId.value = cluster.id;
        document.getElementById('clusterName').value = cluster.clusterName || '';
        document.getElementById('clusterDescription').value = cluster.description || '';
        document.getElementById('clusterRecipientEmails').value = cluster.recipientEmails || '';

        if (clusterModalTitle) clusterModalTitle.innerHTML = '<i class="fa-solid fa-pen-to-square"></i> Edit Cluster Details';
        if (saveClusterBtn) saveClusterBtn.textContent = 'Update Cluster';

        openModal(clusterModal);
    };

    if (openCertModalBtn) {
        openCertModalBtn.addEventListener('click', () => {
            resetCertModal();
            openModal(certModal);
        });
    }
    if (closeCertModalBtn) closeCertModalBtn.addEventListener('click', () => closeModal(certModal));
    if (cancelCertModalBtn) cancelCertModalBtn.addEventListener('click', () => closeModal(certModal));

    window.openAddCertForCluster = (clusterId) => {
        resetCertModal();
        certClusterSelect.value = clusterId;
        openModal(certModal);
    };

    window.editCertificate = (clusterId, certId) => {
        const cluster = clustersData.find(c => c.id === clusterId);
        if (!cluster) return;
        const cert = cluster.certificates ? cluster.certificates.find(c => c.id === certId) : null;
        if (!cert) return;

        editCertId.value = cert.id;
        editCertClusterId.value = cluster.id;
        certNameInput.value = cert.certificateName;
        certIssuedDateInput.value = cert.issuedDate;
        certEndDateInput.value = cert.endDate;

        certModalTitle.innerHTML = '<i class="fa-solid fa-pen-to-square"></i> Edit Certificate';
        certClusterGroup.style.display = 'none';
        certClusterSelect.required = false;

        openModal(certModal);
    };

    function resetCertModal() {
        createCertForm.reset();
        editCertId.value = '';
        editCertClusterId.value = '';
        certModalTitle.innerHTML = '<i class="fa-solid fa-certificate"></i> Add Certificate to Cluster';
        certClusterGroup.style.display = 'block';
        certClusterSelect.required = true;
    }

    // Form Submissions
    createClusterForm.addEventListener('submit', async (e) => {
        e.preventDefault();
        const submitBtn = createClusterForm.querySelector('button[type="submit"]');
        const origText = submitBtn ? submitBtn.innerHTML : '';
        if (submitBtn) {
            submitBtn.disabled = true;
            submitBtn.innerHTML = '<i class="fa-solid fa-spinner fa-spin"></i> Saving...';
        }

        const clusterId = editClusterId ? editClusterId.value : '';
        const isEdit = clusterId !== '';

        const payload = {
            clusterName: document.getElementById('clusterName').value.trim(),
            description: document.getElementById('clusterDescription').value.trim(),
            recipientEmails: document.getElementById('clusterRecipientEmails').value.trim()
        };

        const url = isEdit ? `/api/clusters/${clusterId}` : '/api/clusters';
        const method = isEdit ? 'PUT' : 'POST';

        try {
            const res = await fetch(url, {
                method: method,
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(payload)
            });

            if (res.ok) {
                closeModal(clusterModal);
                resetClusterModal();
                fetchClusters();
            } else {
                alert(`Failed to ${isEdit ? 'update' : 'save'} cluster.`);
            }
        } catch (err) {
            alert('Error connecting to server: ' + err.message);
        } finally {
            if (submitBtn) {
                submitBtn.disabled = false;
                submitBtn.innerHTML = origText;
            }
        }
    });

    createCertForm.addEventListener('submit', async (e) => {
        e.preventDefault();
        const submitBtn = createCertForm.querySelector('button[type="submit"]');
        const origText = submitBtn ? submitBtn.innerHTML : '';
        if (submitBtn) {
            submitBtn.disabled = true;
            submitBtn.innerHTML = '<i class="fa-solid fa-spinner fa-spin"></i> Saving...';
        }

        const isEdit = editCertId.value !== '';

        try {
            if (isEdit) {
                const clusterId = editCertClusterId.value;
                const certId = editCertId.value;
                const payload = {
                    clusterId: clusterId,
                    certificateName: certNameInput.value.trim(),
                    issuedDate: certIssuedDateInput.value,
                    endDate: certEndDateInput.value
                };

                const res = await fetch(`/api/clusters/${clusterId}/certificates/${certId}`, {
                    method: 'PUT',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify(payload)
                });

                if (res.ok) {
                    closeModal(certModal);
                    resetCertModal();
                    fetchClusters();
                } else {
                    alert('Failed to update certificate.');
                }
            } else {
                const payload = {
                    clusterId: certClusterSelect.value,
                    certificateName: certNameInput.value.trim(),
                    issuedDate: certIssuedDateInput.value,
                    endDate: certEndDateInput.value
                };

                const res = await fetch('/api/clusters/certificates', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify(payload)
                });

                if (res.ok) {
                    closeModal(certModal);
                    resetCertModal();
                    fetchClusters();
                } else {
                    alert('Failed to add certificate.');
                }
            }
        } catch (err) {
            alert('Error processing certificate: ' + err.message);
        } finally {
            if (submitBtn) {
                submitBtn.disabled = false;
                submitBtn.innerHTML = origText;
            }
        }
    });

    window.deleteCertificate = async (clusterId, certId) => {
        try {
            console.log('Deleting certificate:', certId, 'from cluster:', clusterId);
            const res = await fetch(`/api/clusters/${clusterId}/certificates/${certId}`, { method: 'DELETE' });
            if (res.ok) {
                await fetchClusters();
            } else {
                alert('Failed to delete certificate from server.');
            }
        } catch (err) {
            console.error('Error deleting certificate:', err);
            alert('Failed to delete certificate: ' + err.message);
        }
    };

    window.deleteCluster = async (clusterId) => {
        try {
            console.log('Deleting cluster:', clusterId);
            const res = await fetch(`/api/clusters/${clusterId}`, { method: 'DELETE' });
            if (res.ok) {
                await fetchClusters();
            } else {
                alert('Failed to delete cluster from server.');
            }
        } catch (err) {
            console.error('Error deleting cluster:', err);
            alert('Failed to delete cluster: ' + err.message);
        }
    };



    // --- AUTHENTICATION LOGIC ---
    const loginScreen = document.getElementById('loginScreen');
    const loginForm = document.getElementById('loginForm');
    const usernameInput = document.getElementById('usernameInput');
    const passwordInput = document.getElementById('passwordInput');
    const loginErrorMsg = document.getElementById('loginErrorMsg');
    const loginSubmitBtn = document.getElementById('loginSubmitBtn');
    const logoutBtn = document.getElementById('logoutBtn');
    const loggedInUserSpan = document.getElementById('loggedInUser');

    async function checkAuthStatus() {
        try {
            const res = await fetch('/api/auth/status');
            const data = await res.json();
            if (data.authenticated) {
                if (loggedInUserSpan) loggedInUserSpan.textContent = data.username;
                if (loginScreen) loginScreen.classList.add('hidden');
                fetchClusters();
            } else {
                if (loginScreen) loginScreen.classList.remove('hidden');
            }
        } catch (err) {
            console.error('Failed to check auth status:', err);
            if (loginScreen) loginScreen.classList.remove('hidden');
        }
    }

    if (loginForm) {
        loginForm.addEventListener('submit', async (e) => {
            e.preventDefault();
            if (loginErrorMsg) loginErrorMsg.classList.add('hidden');
            if (loginSubmitBtn) {
                loginSubmitBtn.disabled = true;
                loginSubmitBtn.innerHTML = '<i class="fa-solid fa-spinner fa-spin"></i> Authenticating...';
            }

            const username = usernameInput.value.trim();
            const password = passwordInput.value.trim();

            try {
                const res = await fetch('/api/auth/login', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({ username, password })
                });

                const data = await res.json();
                if (data.authenticated) {
                    if (loggedInUserSpan) loggedInUserSpan.textContent = data.username;
                    if (loginScreen) loginScreen.classList.add('hidden');
                    loginForm.reset();
                    fetchClusters();
                } else {
                    if (loginErrorMsg) {
                        loginErrorMsg.innerHTML = '<i class="fa-solid fa-triangle-exclamation"></i> Invalid username or password';
                        loginErrorMsg.classList.remove('hidden');
                    }
                }
            } catch (err) {
                if (loginErrorMsg) {
                    loginErrorMsg.innerHTML = '<i class="fa-solid fa-triangle-exclamation"></i> Server error during authentication';
                    loginErrorMsg.classList.remove('hidden');
                }
            } finally {
                if (loginSubmitBtn) {
                    loginSubmitBtn.disabled = false;
                    loginSubmitBtn.innerHTML = '<i class="fa-solid fa-right-to-bracket"></i> Sign In to Dashboard';
                }
            }
        });
    }

    if (logoutBtn) {
        logoutBtn.addEventListener('click', async () => {
            try {
                await fetch('/api/auth/logout', { method: 'POST' });
            } catch (e) { }
            if (loginScreen) loginScreen.classList.remove('hidden');
        });
    }

    // Initial Auth Check
    checkAuthStatus();
});
