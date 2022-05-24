import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.util.Set;

@Data
public class Candidates {

    Set<Candidate> edited;
    Set<Candidate> added;
    Set<Candidate> removed;
}
