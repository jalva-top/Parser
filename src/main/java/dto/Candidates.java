package dto;

import lombok.Data;

import java.util.Set;

@Data
public class Candidates {
    Set<Candidate> edited;
    Set<Candidate> added;
    Set<Candidate> removed;
}
