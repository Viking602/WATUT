package dev.viking602.watut.probe.mixin;
import dev.viking602.watut.probe.ClientProbe;
import com.corosus.watut.particle.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(value=ParticleRotating.class,remap=false)
public abstract class ParticleProbeMixin {
 @Inject(method="extract",at=@At("TAIL"),remap=false)
 private void probe$extracted(CallbackInfo ci){ClientProbe.particleFrames++;if((Object)this instanceof ParticleDynamic)ClientProbe.dynamicFrames++;}
}
