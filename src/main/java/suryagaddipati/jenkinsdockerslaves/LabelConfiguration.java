package suryagaddipati.jenkinsdockerslaves;

import org.apache.commons.lang.StringUtils;
import org.kohsuke.stapler.DataBoundConstructor;

public   class LabelConfiguration {
        String image;
        String hostBinds;

    @DataBoundConstructor
    public LabelConfiguration(String image, String hostBinds, String label) {
        this.image = image;
        this.hostBinds = hostBinds;
        this.label = label;
    }

    private String label;


        public String getLabel() {
            return label;
        }

        public void setLabel(String label) {
            this.label = label;
        }
        public void setHostBinds(String hostBinds) {
            this.hostBinds = hostBinds;
        }

        public void setImage(String image) {
            this.image = image;
        }
        public String getHostBinds() {
            return hostBinds;
        }

        public String getImage() {
            return image;
        }
        public String[] getHostBindsConfig() {
            return StringUtils.isEmpty( this.hostBinds)? new String[]{}: this.hostBinds.split(" ");
        }
    }
