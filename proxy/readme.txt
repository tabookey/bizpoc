pass-through proxy, to expose a single IP to bitgo.

supports greenlock dynamically-set certificates (enabled if launched on port 443)
Installed on bizpoc.ddns.tabookey.com (AWS instance)

Above DNS name is hard-coded into the client, and is completely static (A record)

== To install:
- add the following to .ssh/config:
  host bizpoc
        hostname bizpoc.ddns.tabookey.com
        user ubuntu
        ForwardAgent yes

scp package.json *service *sh *js bizpoc:proxy/
ssh bizpoc

#NOTE: if the server's DNS name is anything other than "bizpoc.ddns.tabookey.com", then "start.sh" should be updated,
# so that let's encrypt will fetch cert for the right host name.

sudo -s
cp bizpoc-proxy.service /lib/systemd/system/
systemctl start bizpoc-proxy
systemctl enable bizpoc-proxy

view log (no need root)
journalctl -u bizpoc-proxy -f

NOTE: greenlock creates certificate, and automatically updates it.
debug server (port 8090) uses the same certificate, but doesn't get it update.
if the debug server has stale certificate, then simply stop/start the service, so it will re-read new one:

sudo systemctl restart bizpoc-proxy
