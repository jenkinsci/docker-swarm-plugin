package suryagaddipati.jenkinsdockerslaves;

import hudson.model.Node;
import hudson.model.labels.LabelAtom;

public class DockerMachineLabel extends LabelAtom {
    public DockerMachineLabel(String name) {
        super(name);
    }

    @Override
    public boolean contains(Node node) {
        return this.name.equals(node.getNodeName());
    }
}
