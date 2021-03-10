package moderator;

import net.dv8tion.jda.api.entities.Guild;

public class Player {

    private final long id;
    private final long coachId;

    public Player(long id, long coachId) {
        this.id = id;
        this.coachId = coachId;
    }

    public Player(long id) {
        this(id, -1);
    }

    public String toString(Guild guild) {
        return guild.getMemberById(id).getAsMention() + (isBot() ? String.format(" (Coach: %s)", guild.getMemberById(coachId).getAsMention()) : "");
    }

    public long getId() {
        return id;
    }

    public long getCoachId() {
        return coachId;
    }

    public boolean isBot() {
        return coachId != -1;
    }
}
