pass-through proxy, to expose a single IP to bitgo.

supports greenlock dynamically-set certificates (enabled if launched on port 443)
installed on bizpoc.ddns.tabookey.com (AWS instance)

to install:

(as root)
cp bizpoc-proxy.service /lib/systemd/system/
systemctl start bizpoc-proxy
systemctl enable bizpoc-proxy

view log:
journalctl -u bizpoc-proxy
