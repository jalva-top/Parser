import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import java.time.ZoneId;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.stream.Collectors;

/**
 *
 */
public class Parser {

    public static final String META = "meta";
    public static final String CANDIDATES = "candidates";
    public static final String ID = "id";

    final ZoneId currentZone;

    public Parser(ZoneId currentZone) {
        this.currentZone = currentZone;
    }

    /**
     * Structure of before and after have to be the same:
     * <pre>
     * meta: {}
     * candidates: [ {id: <number>, otherField: "value"} ]
     * <pre/>
     * @param before Older object state
     * @param after Newer object state
     *
     * @return JSONObject represents changes between before and after states
     */
    public JSONObject parse(JSONObject before, JSONObject after) {

        JSONObject metaBefore = getJSONObject(before, META);
        JSONObject metaAfter = getJSONObject(after, META);
        final List<MetaItem> metaDiff = createMetaDiff(metaBefore, metaAfter);

        final JSONArray candidatesBefore = getCandidates(before);
        final JSONArray candidatesAfter = getCandidates(after);
        final Candidates candidatesDiff = createCandidatesDiff(candidatesBefore, candidatesAfter);

        final Result result = new Result(metaDiff, candidatesDiff);

        return JSONObject.fromObject(result);
    }
    
    private List<MetaItem> createMetaDiff(JSONObject metaBefore, JSONObject metaAfter) {
        final List<MetaItem> meta = new ArrayList<>();

        Set<String> keys = metaBefore.keySet();
        for (String key : keys) {
            Object valueBefore = metaBefore.get(key);
            Object valueAfter = metaAfter.get(key);

            if (!Objects.equals(valueBefore, valueAfter)) {
                MetaItem item = new MetaItem();
                item.setField(key);
                item.setBefore(asString(valueBefore));
                item.setAfter(asString(valueAfter));

                meta.add(item);
            }
        }

        return meta;
    }

    private String asString(Object obj) {
        String value = Objects.toString(obj);
        return dateTimeAsString(value).orElse(value);
    }

    private Optional<String> dateTimeAsString(String value) {
        try {
            return Optional.of(zonedDateTimeAsString(value));
        } catch (DateTimeParseException e) {
            //cannot be parsed as date
        }

        return Optional.empty();
    }

    private String zonedDateTimeAsString(String value) {
        ZonedDateTime zonedDateTime = ZonedDateTime.parse(value);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssx").withZone(currentZone);

        return zonedDateTime.format(formatter);
    }

    private Candidates createCandidatesDiff(JSONArray candidatesBefore, JSONArray candidatesAfter) {
        final Set<Long> idsBefore = getIds(candidatesBefore);
        final Set<Long> idsAfter = getIds(candidatesAfter);

        final Set<Candidate> edited = findEdited(candidatesBefore, candidatesAfter);
        final Set<Candidate> added = findIdsExistOnlyInFirstSetAndMapToCandidate(idsAfter, idsBefore);
        final Set<Candidate> removed = findIdsExistOnlyInFirstSetAndMapToCandidate(idsBefore, idsAfter);

        final Candidates candidates = new Candidates();
        candidates.setEdited(edited);
        candidates.setAdded(added);
        candidates.setRemoved(removed);

        return candidates;
    }

    private Set<Candidate> findEdited(JSONArray candidatesBefore, JSONArray candidatesAfter) {
        final Set<Long> idsBefore = getIds(candidatesBefore);
        final Set<Long> idsAfter = getIds(candidatesAfter);

        return idsBefore.stream()
                .filter(idsAfter::contains)
                .filter(id -> {
                    JSONObject candidateBefore = findCandidate(candidatesBefore, id);
                    JSONObject candidateAfter = findCandidate(candidatesAfter, id);

                    return isEdited(candidateBefore, candidateAfter);
                })
                .map(Candidate::new)
                .collect(Collectors.toSet());
    }

    private boolean isEdited(JSONObject before, JSONObject after) {
        Set<String> keys = new HashSet<>(before.keySet());
        keys.remove(Parser.ID);

        for (String key : keys) {
            Object valueBefore = before.get(key);
            Object valueAfter = after.get(key);

            if (!Objects.equals(valueBefore, valueAfter)) {
                return true;
            }
        }

        return false;
    }

    private Set<Candidate> findIdsExistOnlyInFirstSetAndMapToCandidate(Set<Long> first, Set<Long> second) {
        return first.stream().filter(c -> !second.contains(c)).map(Candidate::new).collect(Collectors.toSet());
    }

    private Set<Long> getIds(JSONArray arr) {
        Set<Long> ids = new HashSet<>(arr.size());
        for (int i = 0; i < arr.size(); i++) {
            long id = getId(arr.getJSONObject(i));
            ids.add(id);
        }

        return Collections.unmodifiableSet(ids);
    }

    private long getId(JSONObject data) {
        return data.getLong(ID);
    }

    private JSONObject findCandidate(JSONArray arr, long id) {
        for (int i = 0; i < arr.size(); i++) {
            JSONObject cur = arr.getJSONObject(i);
            if (id == getId(cur)) {
                return cur;
            }
        }

        return null;
    }

    private JSONArray getCandidates(JSONObject data) {
        return data.getJSONArray(CANDIDATES);
    }

    private JSONObject getJSONObject(JSONObject data, String key) {
        if (!data.containsKey(key)) {
            throw new IllegalArgumentException("JSONObject have to contain fields '" + key + "'");
        }
        return data.getJSONObject(key);
    }
}
