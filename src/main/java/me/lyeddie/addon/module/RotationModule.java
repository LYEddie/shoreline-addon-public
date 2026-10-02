package me.lyeddie.addon.module;

import me.lyeddie.addon.managers.Managers;
import me.lyeddie.addon.managers.impl.util.Rotation;
import meteordevelopment.meteorclient.systems.modules.Category;

public class RotationModule extends AddonModule {
    private final int rotationPriority;

    public RotationModule(Category category, String name, String description) {
        super(category, name, description);
        this.rotationPriority = 100;
    }

    public RotationModule(Category category, String name, String description, int rotationPriority) {
        super(category, name, description);
        this.rotationPriority = rotationPriority;
    }

    protected void setRotation(float yaw, float pitch) {
        Managers.ROTATION.setRotation(new Rotation(getRotationPriority(), yaw, pitch));
    }

    protected void setRotationSilent(float yaw, float pitch) {
        Managers.ROTATION.setRotationSilent(yaw, pitch);
    }

    protected void setRotationClient(float yaw, float pitch) {
        Managers.ROTATION.setRotationClient(yaw, pitch);
    }

    protected boolean isRotationBlocked() {
        return Managers.ROTATION.isRotationBlocked(getRotationPriority());
    }

    protected int getRotationPriority() {
        return rotationPriority;
    }
}
