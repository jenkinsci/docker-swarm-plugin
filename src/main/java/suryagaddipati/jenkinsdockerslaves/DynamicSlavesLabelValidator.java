package suryagaddipati.jenkinsdockerslaves;

import hudson.Extension;
import hudson.model.AbstractProject;
import hudson.model.Label;
import hudson.util.FormValidation;

import javax.annotation.Nonnull;

//@Extensio/
public class DynamicSlavesLabelValidator extends AbstractProject.LabelValidator {
    @Nonnull
    @Override
    public FormValidation check(@Nonnull AbstractProject<?, ?> project, @Nonnull Label label) {
        return label.getName().equals("meow")? FormValidation.ok(): null;
    }
}
