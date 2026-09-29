package pixlepix.auracascade.qa.neoforge.energy;

import java.nio.file.Path;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

final class EnergyInteropQaCommand {
    private EnergyInteropQaCommand() {
    }

    static void register(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("aura_qa_energy")
            .requires(source -> source.hasPermission(4))
            .executes(context -> {
                Path outputDirectory = outputDirectory(context.getSource().getServer().getServerDirectory());
                if (!EnergyInteropRuntimeFixture.start(context.getSource().getLevel(), outputDirectory)) {
                    context.getSource().sendFailure(Component.literal(
                        "Aura energy QA did not start; see the server log or energy-result.json."));
                    return 0;
                }
                context.getSource().sendSuccess(() -> Component.literal(
                    "Aura energy QA started in this disposable world; results will be written to energy-result.json."), true);
                return 1;
            }));
    }

    private static Path outputDirectory(Path serverDirectory) {
        String configured = System.getProperty("aura.qa.energy.dir");
        if (configured == null || configured.isBlank()) {
            configured = System.getProperty("aura.qa.observer.dir");
        }
        return configured == null || configured.isBlank()
            ? serverDirectory.resolve("qa-energy")
            : Path.of(configured);
    }
}
