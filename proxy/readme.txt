pass-through proxy, to expose a single IP to bitgo.

supports greenlock dynamically-set certificates (enabled if launched on port 443)
installed on bizpoc.ddns.tabookey.com (AWS instance)

to install:

(as root)
cp bizpoc-proxy.service /lib/systemd/system/
systemctl start bizpoc-proxy
systemctl enable bizpoc-proxy

view log:
journalctl -u bizpoc-proxy -f

NOTE: greenlock creates certificate, and automatically updates it.
debug server (port 8090) uses the same certificate, but doesn't get it update.
if the debug server has stale certificate, then simply stop/start the service, so it will re-read new one.
