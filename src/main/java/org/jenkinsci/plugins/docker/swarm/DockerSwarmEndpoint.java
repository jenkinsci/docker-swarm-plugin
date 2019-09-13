package org.jenkinsci.plugins.docker.swarm;

import static hudson.Util.fixEmpty;

import java.util.List;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

import com.cloudbees.plugins.credentials.common.StandardListBoxModel;
import com.cloudbees.plugins.credentials.domains.DomainRequirement;
import com.cloudbees.plugins.credentials.domains.URIRequirementBuilder;

import org.jenkinsci.plugins.docker.commons.credentials.DockerServerDomainRequirement;
import org.jenkinsci.plugins.docker.commons.credentials.DockerServerEndpoint;
import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;

import hudson.Extension;
import hudson.model.AbstractDescribableImpl;
import hudson.model.Descriptor;
import hudson.model.Item;
import hudson.util.ListBoxModel;
import jenkins.model.Jenkins;

public class DockerSwarmEndpoint extends AbstractDescribableImpl<DockerSwarmEndpoint> {
    private final String uri;
    private final @CheckForNull String credentialsId;

    @DataBoundConstructor
    public DockerSwarmEndpoint(String uri, String credentialsId) {
        this.uri = fixEmpty(uri);
        this.credentialsId = fixEmpty(credentialsId);
    }

    public @Nullable String getUri() {
        return uri;
    }

    public @Nullable String getCredentialsId() {
        return credentialsId;
    }

    @Override
    public String toString() {
        return "DockerSwarmEndpoint[" + uri + ";credentialsId=" + credentialsId + "]";
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 13 * hash + (this.uri != null ? this.uri.hashCode() : 0);
        hash = 13 * hash + (this.credentialsId != null ? this.credentialsId.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final DockerSwarmEndpoint other = (DockerSwarmEndpoint) obj;
        if ((this.uri == null) ? (other.uri != null) : !this.uri.equals(other.uri)) {
            return false;
        }
        if ((this.credentialsId == null) ? (other.credentialsId != null)
                : !this.credentialsId.equals(other.credentialsId)) {
            return false;
        }
        return true;
    }

    @Extension
    public static class DescriptorImpl extends Descriptor<DockerSwarmEndpoint> {
        @Override
        public String getDisplayName() {
            return "Docker Swarm";
        }

        public ListBoxModel doFillCredentialsIdItems(@AncestorInPath Item item, @QueryParameter String uri) {
            if (item == null && !Jenkins.getInstance().hasPermission(Jenkins.ADMINISTER) ||
                item != null && !item.hasPermission(Item.EXTENDED_READ)) {
                return new StandardListBoxModel();
            }
            
            return new StandardListBoxModel()
                    .includeEmptyValue()
                    .withMatching(AuthenticationTokens.matcher(DockerRegistryToken.class),
                            CredentialsProvider.lookupCredentials(
                                    StandardCredentials.class,
                                    item,
                                    null,
                                    Collections.<DomainRequirement>emptyList()
                            )
                    );



            final DockerServerEndpoint.DescriptorImpl descriptor = (DockerServerEndpoint.DescriptorImpl) Jenkins
                    .getInstance().getDescriptorOrDie(DockerServerEndpoint.class);
            return descriptor.doFillCredentialsIdItems(item, uri);
        }

    }
}
