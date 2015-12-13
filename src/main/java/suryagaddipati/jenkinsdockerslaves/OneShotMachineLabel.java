package suryagaddipati.jenkinsdockerslaves;

import hudson.model.Node;
import hudson.model.labels.LabelAtom;

public class OneShotMachineLabel extends LabelAtom {
    public OneShotMachineLabel(String name) {
        super(name);
    }

    @Override
    public boolean contains(Node node) {
        return this.name.equals(node.getNodeName());
    }
}
