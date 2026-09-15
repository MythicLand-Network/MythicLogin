package cn.mythicland.mythiclogin.velocity;

import cn.mythicland.framework.component.InjectComponent;

@InjectComponent
public final class VelocityOnlyComponent {
    public VelocityOnlyComponent(VelocityOnlyDependency dependency) {
    }
}

interface VelocityOnlyDependency {
}
