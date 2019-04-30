package io.openems.edge.common.access_control;

import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.value.Value;
import io.openems.edge.common.component.OpenemsComponent;

import javax.naming.AuthenticationException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

public class AccessControl {

    private final Set<User> users = new HashSet<>();

    private final Set<Role> roles = new HashSet<>();

    private final Set<Group> groups = new HashSet<>();

    protected volatile List<OpenemsComponent> components = new ArrayList<>();

    public AccessControl() {
    }

    public void tempSetupAccessControl(Set<Role> roles, List<OpenemsComponent> components, Set<User> users) {
        this.components = components;
        this.roles.clear();
        this.roles.addAll(roles);
        this.users.clear();
        this.users.addAll(users);
    }

    public Role login(String username, String password, Long roleId) throws AuthenticationException {
        User matchingUser = users.stream().filter(userNew -> (userNew.getUsername().equals(username) && userNew.getPassword() == password)).findFirst().orElseThrow(AuthenticationException::new);
        return matchingUser.getRoles().stream().filter(r -> (r.getId().equals(roleId))).findFirst().orElseThrow(AuthenticationException::new);
    }

    public List<? extends Value<?>> getChannelValues(Role role) throws AuthenticationException {
        // fetch our own role in case someone made it to fake one!
        Role roleInternal = roles.stream().filter(ro -> ro.equals(role)).findFirst().orElseThrow(AuthenticationException::new);
        final List<? extends Value<?>>[] values = new List[]{null};
        components.forEach(openemsComponent -> values[0] = openemsComponent.channels().stream().filter(channel ->
        {
            AtomicBoolean allowed = new AtomicBoolean(false);
            roleInternal.getPermissions(channel.address()).ifPresent(permissions -> allowed.set(permissions.contains(Permission.READ)));
            return allowed.get();
        }).map(Channel::value).collect(Collectors.toList()));
        return values[0];
    }

    public void addGroup(Group newGroup) {
        this.groups.add(newGroup);
    }

    public Set<Group> getGroups() {
        return groups;
    }

    public Set<Role> getRoles() {
        return roles;
    }

    public void addRole(Role role) {
        this.roles.add(role);
    }

    public void addUser(User user) {
        this.users.add(user);
    }
}
