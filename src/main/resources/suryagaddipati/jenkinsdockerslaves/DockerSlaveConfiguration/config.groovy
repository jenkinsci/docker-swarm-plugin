package suryagaddipati.jenkinsdockerslaves.DockerSlaveConfiguration

def f = namespace(lib.FormTagLib)
f.section(title:_("Docker Slaves Configuration")) {
    f.block {
        f.entry(title:_("Job Label"), field:"label") {
            f.textbox()
        }

        f.entry(title:_("Docker http(s) uri"), field:"uri") {
            f.textbox()
        }
        f.entry(field: "useTLS") {
            f.checkbox(title: _("Use TLS?"))
        }

        f.entry(title:_("Certificates Path"), field:"certificatesPath") {
            f.textbox()
        }

        f.entry(title:_("Image"), field:"image") {
            f.textbox()
        }
        f.entry(title:_("Host Binds( space seperated)"), field:"hostBinds") {
            f.textbox()
        }
        f.entry(title:_("Jenkins Url"), field:"jenkinsUrl") {
            f.textbox()
        }
        f.entry(field: "privileged") {
            f.checkbox(title: _("privileged"))
        }



    }
}
