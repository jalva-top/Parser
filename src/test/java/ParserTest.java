
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import dto.Candidates;
import dto.MetaItem;
import dto.Result;
import net.sf.json.JSONObject;
import org.apache.commons.io.FileUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.time.ZoneId;
import java.util.List;

/**
 *
 */
public class ParserTest {

    Parser parser = new Parser(ZoneId.of("Europe/Kiev"));
    Gson gson = new GsonBuilder().create();

    JSONObject before;
    JSONObject after;
    JSONObject expectedDiff;

    @Before
    public void setup() throws IOException {
        before = parseFileToJSONObject("before.json");
        after = parseFileToJSONObject("after.json");
        expectedDiff = parseFileToJSONObject("diff.json");
    }

    JSONObject parseFileToJSONObject(String filename) throws IOException {
        String content = getFileContent(filename);
        return JSONObject.fromObject(content);
    }

    String getFileContent(String filename) throws IOException {
        File file = new File("src/test/resources/" + filename);
        return FileUtils.readFileToString(file);
    }

    @Test
    public void testParse() {
        //when:
        JSONObject diff = parser.parse(before, after);

        Result actual = parse(diff);
        Result expected = parse(expectedDiff);

        Candidates actualCandidates = actual.getCandidates();
        Candidates expectedCandidates = expected.getCandidates();

        List<MetaItem> actualMeta = actual.getMeta();
        List<MetaItem> expectedMeta = expected.getMeta();

        //then:
        Assert.assertEquals(expectedCandidates.getAdded(), actualCandidates.getAdded());
        Assert.assertEquals(expectedCandidates.getRemoved(), actualCandidates.getRemoved());
        Assert.assertEquals(expectedCandidates.getEdited(), actualCandidates.getEdited());

        Assert.assertEquals(actualMeta.size(), expectedMeta.size());

        for (MetaItem expectedItem : expectedMeta) {
            MetaItem actualItem = findByField(actualMeta, expectedItem.getField());

            Assert.assertEquals(expectedItem.getBefore(), actualItem.getBefore());
            Assert.assertEquals(expectedItem.getAfter(), actualItem.getAfter());
        }
    }

    MetaItem findByField(List<MetaItem> meta, String field) {
        return meta.stream().filter(item-> field.equals(item.getField())).findAny().orElse(null);
    }

    Result parse(JSONObject data) {
        return gson.fromJson(data.toString(), Result.class);
    }

}
