/* Public same-origin API routes only. Credentials are never browser settings. */
(() => {
  if (window.location.protocol !== 'https:') {
    const explain = () => {
      const message = document.createElement('p');
      message.textContent = 'Secure access is not enabled at this HTTP address. Open the trusted HTTPS public-IP URL after TLS setup. Sign-in and JWT traffic remain blocked over HTTP.';
      message.setAttribute('role', 'alert');
      document.body.replaceChildren(message);
    };
    if (document.readyState === 'loading') document.addEventListener('DOMContentLoaded', explain, {once: true});
    else explain();
    throw new Error('HTTPS is required; no HTTP API URLs were configured.');
  }
  const origin = window.location.origin;
  window.MED_SUPPORT_CONFIG = Object.freeze({
    apiBaseUrls: Object.freeze({
      identity: origin + '/identity',
      booking: origin + '/booking',
      bloodRequest: origin + '/blood',
      notification: origin + '/notification'
    })
  });
})();
