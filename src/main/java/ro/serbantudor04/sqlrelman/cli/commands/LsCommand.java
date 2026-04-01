package ro.serbantudor04.sqlrelman.cli.commands;

import ro.serbantudor04.sqlrelman.cli.annotations.Command;
import ro.serbantudor04.sqlrelman.engine.models.Patch;
import ro.serbantudor04.sqlrelman.engine.models.Release;

import java.util.List;

/**
 * ls          — tree of all releases and their patches
 * ls <version> — patches of a single release
 */
@Command
public class LsCommand extends BaseCommand {

    @Override
    public void run(List<String> args) {
        if (args.isEmpty()) {
            listAll();
        } else {
            listRelease(args.get(0));
        }
    }

    private void listAll() {
        List<Release> releases = Release.listAll();
        if (releases.isEmpty()) {
            System.out.println("No releases found.");
            return;
        }

        for (int i = 0; i < releases.size(); i++) {
            Release r = releases.get(i);
            boolean lastRelease = (i == releases.size() - 1);
            String releasePrefix = lastRelease ? "└── " : "├── ";
            String childIndent   = lastRelease ? "    " : "│   ";

            List<Patch> patches = r.getSortedPatches();
            System.out.printf("%s%s/  (%d patch%s)%n",
                    releasePrefix, r.getVersion(),
                    patches.size(), patches.size() == 1 ? "" : "es");

            for (int j = 0; j < patches.size(); j++) {
                Patch p = patches.get(j);
                boolean lastPatch = (j == patches.size() - 1);
                String patchPrefix = childIndent + (lastPatch ? "└── " : "├── ");
                System.out.printf("%s[%03d] %s%n", patchPrefix, p.getOrder(), p.getId());
            }
        }
    }

    private void listRelease(String version) {
        Release release = Release.find(version);
        if (release == null) {
            System.err.println("Release '" + version + "' not found.");
            return;
        }

        List<Patch> patches = release.getSortedPatches();
        System.out.printf("%s/  —  %s%n", release.getVersion(), release.getDescription());

        if (patches.isEmpty()) {
            System.out.println("└── (no patches)");
            return;
        }

        for (int i = 0; i < patches.size(); i++) {
            Patch p = patches.get(i);
            boolean last = (i == patches.size() - 1);
            System.out.printf("%s[%03d] %s  —  %s%n",
                    last ? "└── " : "├── ",
                    p.getOrder(), p.getId(), p.getDescription());
        }
    }

    @Override public String getName()        { return "ls"; }
    @Override public String getDescription() { return "Tree view of releases and patches."; }
    @Override public String getUsage()       { return "ls [version]"; }
    @Override public String getHelp()        { return "ls → all releases. ls <version> → patches in that release."; }
    @Override public String getVersion()     { return "1.0"; }
    @Override public String getAuthor()      { return "Serban Tudor"; }
    @Override public String getDate()        { return "2026-04-01"; }
    @Override public String getLicense()     { return "MIT"; }
}