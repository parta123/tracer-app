# Security

Do not commit database dumps, SSH keys, Cloudflare credentials, API session
headers, HA shared secrets, production environment files, or server installer
packages.

If a credential is committed accidentally, revoke and rotate it immediately;
removing it in a later commit is not sufficient because it remains in Git
history.

