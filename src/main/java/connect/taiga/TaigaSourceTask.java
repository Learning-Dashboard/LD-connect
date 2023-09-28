package connect.taiga;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.*;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import model.taiga.Epic;
import model.taiga.Issue;
import model.taiga.Milestone;
import model.taiga.Project;
import model.taiga.Task;
import model.taiga.UserStory;
import org.apache.kafka.connect.data.Schema;
import org.apache.kafka.connect.data.Struct;
import org.apache.kafka.connect.source.SourceRecord;
import org.apache.kafka.connect.source.SourceTask;

public class TaigaSourceTask extends SourceTask {
    private static final TimeZone tzUTC = TimeZone.getTimeZone("UTC");
    private static final DateFormat dfZULU = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm'Z'");
    private String taigaUrl;
    private List<String> taigaSlug;
    private String taigaUser;
    private String taigaPass;
    private String taigaToken;
    private List<String> issuetopic;
    private List<String> epictopic;
    private List<String> userstorytopic;
    private List<String> tasktopic;
    private String taigaInterval;
    private Integer interval;
    private String taigaTeamsNum;
    private Integer teamsNum;
    private String taigaTeamsInterval;
    private Integer teamsInterval;
    private String taigaTaskCustomAttributes;
    private String taigaUserstoryCustomAttributes;
    private Integer count = 0;
    private static final DateFormat onlyDate;
    private long lastPoll = 0L;
    private int currentTaskID;
    private final Logger log = Logger.getLogger(TaigaSourceTask.class.getName());

    public TaigaSourceTask() {
    }

    private Boolean followsPattern(String description) {
        List<String> patterns = Arrays.asList(
            "AS[\\S\\s]*I WANT[\\S\\s]*SO THAT[\\S\\s]*",
            "AS[\\S\\s]*I WANT[\\S\\s]*TO[\\S\\s]*",
            "COMO[\\S\\s]*QUIERO[\\S\\s]*DE MANERA QUE[\\S\\s]*",
            "COMO[\\S\\s]*QUIERO[\\S\\s]*DE FORMA QUE[\\S\\s]*",
            "COMO[\\S\\s]*QUIERO[\\S\\s]*PARA[\\S\\s]*",
            "COMO[\\S\\s]*QUIERO[\\S\\s]*POR[\\S\\s]*",
            "COMO[\\S\\s]*QUIERO[\\S\\s]*PORQUÉ[\\S\\s]*",
            "COMO[\\S\\s]*QUIERO[\\S\\s]*PORQUE[\\S\\s]*",
            "COM[\\S\\s]*VULL[\\S\\s]*DE MANERA QUE[\\S\\s]*",
            "COM[\\S\\s]*VULL[\\S\\s]*DE FORMA QUE[\\S\\s]*",
            "COM[\\S\\s]*VULL[\\S\\s]*PER[\\S\\s]*",
            "COM[\\S\\s]*VULL[\\S\\s]*PERQUE[\\S\\s]*",
            "COM[\\S\\s]*VULL[\\S\\s]*PERQUÈ[\\S\\s]*",
            "COM[\\S\\s]*VULL[\\S\\s]*PERQUÉ[\\S\\s]*"
        );
        for (String pattern : patterns) {
            Pattern regex = Pattern.compile(pattern, Pattern.CASE_INSENSITIVE);
            if (regex.matcher(description.toUpperCase()).matches()) return true;
        }
        return false;
    }

    private Boolean hasAcceptanceCriteria(String criteria) {
        return criteria.contains("1.") || criteria.contains("*");
    }

    public List<SourceRecord> poll() throws InterruptedException {
        List<SourceRecord> records = new ArrayList<>();

        this.log.info("lastPollDeltaMillis:" + (System.currentTimeMillis() - this.lastPoll) + " interval:" + this.interval);

        if (this.lastPoll != 0L &&
            System.currentTimeMillis() < ( this.lastPoll + (long) (this.interval * 1000) )
            && this.currentTaskID == 0) {

            this.log.info("------- exit polling, " + (System.currentTimeMillis() - this.lastPoll) / 1000L + " secs since last poll.");
            Thread.sleep(1000L);

        }

        else {
            if (this.currentTaskID == 0) this.lastPoll = System.currentTimeMillis();
            Thread.sleep(this.teamsInterval * 1000L); // Wait between teams

            if (!this.taigaUser.equals("XXXX") && !this.taigaUser.equals(" ") && !this.taigaUser.equals(""))
                this.taigaToken = TaigaApi.Login(this.taigaUrl, this.taigaUser, this.taigaPass);
            else this.taigaToken = null;

            this.log.info("\n\n**********");
            this.log.info("Start executing task " + this.currentTaskID + " with Taiga slug " + this.taigaSlug.get(this.currentTaskID));
            String taigaProjectId = String.valueOf(TaigaApi.getProjectId(this.taigaUrl, this.taigaSlug.get(this.currentTaskID), this.taigaToken));
            Project myProject = TaigaApi.getProject(this.taigaUrl, taigaProjectId, this.taigaToken);
            Issue[] redmineIssues = TaigaApi.getIssuesByProjectId(this.taigaUrl, taigaProjectId, this.taigaToken);
            records.addAll(this.getIssueSourceRecords(redmineIssues, myProject));

            Epic[] myEpics = TaigaApi.getEpicsByProjectID(this.taigaUrl, taigaProjectId, this.taigaToken);
            UserStory[] myUserStories = TaigaApi.getUserStoriesByProjectId(this.taigaUrl, taigaProjectId, this.taigaToken);
            Task[] myTasks = TaigaApi.getTasks(this.taigaUrl, taigaProjectId, this.taigaToken);
            Map<Integer, Milestone> myMilestones = TaigaApi.getMilestonesByProjectId(this.taigaUrl, taigaProjectId, this.taigaToken);

            String[] taskCustomAttributes = this.taigaTaskCustomAttributes.split(",");
            for (int i = 0; i < taskCustomAttributes.length; ++i)
                taskCustomAttributes[i] = taskCustomAttributes[i].toUpperCase();
            Map<Integer, String> taskAttributesIDs = TaigaApi.getCustomAttributesIDs(this.taigaUrl, "task", taigaProjectId, this.taigaToken);

            String[] storyCustomAttributes = this.taigaUserstoryCustomAttributes.split(",");
            for (int i = 0; i < storyCustomAttributes.length; ++i)
                storyCustomAttributes[i] = storyCustomAttributes[i].toUpperCase();
            Map<Integer, String> userstoryAttributesIDs = TaigaApi.getCustomAttributesIDs(this.taigaUrl, "userstory", taigaProjectId, this.taigaToken);

            records.addAll(this.getTaigaEpics(myEpics));
            records.addAll(this.getTaigaUserStories(myUserStories, myMilestones, userstoryAttributesIDs));
            records.addAll(this.getTaigaTasks(myTasks, taskAttributesIDs, myMilestones));

            this.log.info("Finished executing task " + this.currentTaskID +" with Taiga slug " + this.taigaSlug.get(this.currentTaskID));
            this.log.info("**********\n\n");

            ++this.currentTaskID;
            if (this.currentTaskID == this.teamsNum) this.currentTaskID = 0;
        }
        return records;
    }

    private List<SourceRecord> getTaigaTasks(Task[] tasks, Map<Integer, String> taskAttributesIDs, Map<Integer, Milestone> milestones) throws InterruptedException {
        List<SourceRecord> result = new ArrayList<>();

        for(Task t : tasks) {
            if (this.count == 5) {
                Thread.sleep(10L);
                this.count = 0;
            }

            this.count = this.count + 1;
            this.log.info("TASK ID: " + t.id);
            this.log.info("TASK URL: " + this.taigaUrl + "/task/" + t.id);
            Struct tasktemp = new Struct(TaigaSchema.taigaTask);
            tasktemp.put("subject", t.subject);
            if (t.user_story != null) {
                tasktemp.put("user_story_id", t.user_story);
            }

            tasktemp.put("id", t.id);
            tasktemp.put("status", t.status_extra_info.name);
            tasktemp.put("is_closed", t.is_closed);
            tasktemp.put("reference", t.ref);
            if (t.assigned_to_extra_info != null) {
                tasktemp.put("assigned", t.assigned_to_extra_info.username);
            }

            tasktemp.put("created_date", dfZULU.format(t.created_date));
            if (t.modified_date != null) {
                tasktemp.put("modified_date", dfZULU.format(t.modified_date));
            }

            if (t.finished_date != null) {
                tasktemp.put("finished_date", dfZULU.format(t.finished_date));
            }

            Map<String, String> temp = TaigaApi.getCustomAttributes(t.id, "task", this.taigaUrl, this.taigaToken, taskAttributesIDs);
            if (temp.get("ESTIMATED EFFORT") != null) {
                tasktemp.put("estimated_effort", Float.parseFloat(temp.get("ESTIMATED EFFORT")));
            } else if (temp.get("ESFORÇ ESTIMAT") != null) {
                tasktemp.put("estimated_effort", Float.parseFloat(temp.get("ESFORÇ ESTIMAT")));
            } else if (temp.get("ESFUERZO ESTIMADO") != null) {
                tasktemp.put("estimated_effort", Float.parseFloat(temp.get("ESFUERZO ESTIMADO")));
            }

            if (temp.get("ACTUAL EFFORT") != null) {
                tasktemp.put("actual_effort", Float.parseFloat(temp.get("ACTUAL EFFORT")));
            } else if (temp.get("ESFORÇ REAL") != null) {
                tasktemp.put("actual_effort", Float.parseFloat(temp.get("ESFORÇ REAL")));
            } else if (temp.get("ESFUERZO REAL") != null) {
                tasktemp.put("actual_effort", Float.parseFloat(temp.get("ESFUERZO REAL")));
            }

            if (t.milestone != null) {
                tasktemp.put("milestone_id", (milestones.get(t.milestone)).id);
                tasktemp.put("milestone_name", (milestones.get(t.milestone)).name);
                tasktemp.put("milestone_closed_points", (milestones.get(t.milestone)).closed_points);
                tasktemp.put("milestone_total_points", (milestones.get(t.milestone)).total_points);
                LocalDateTime now = LocalDateTime.now();

                try {
                    Date today = onlyDate.parse(now.toString());
                    if (!(milestones.get(t.milestone)).closed && (milestones.get(t.milestone)).estimated_start.compareTo(today) < 0) {
                        tasktemp.put("milestone_closed", (milestones.get(t.milestone)).closed);
                    } else {
                        tasktemp.put("milestone_closed", true);
                    }
                } catch (ParseException e) {
                    tasktemp.put("milestone_closed", (milestones.get(t.milestone)).closed);
                    this.log.info("unable to parse " + now);
                    throw new InterruptedException();
                }

                tasktemp.put("milestone_created_date", dfZULU.format((milestones.get(t.milestone)).created_date));
                if ((milestones.get(t.milestone)).modified_date != null) {
                    tasktemp.put("milestone_modified_date", dfZULU.format((milestones.get(t.milestone)).modified_date));
                }

                if ((milestones.get(t.milestone)).estimated_start != null) {
                    tasktemp.put("estimated_start", dfZULU.format((milestones.get(t.milestone)).estimated_start));
                }

                if ((milestones.get(t.milestone)).estimated_finish != null) {
                    tasktemp.put("estimated_finish", dfZULU.format((milestones.get(t.milestone)).estimated_finish));
                }
            }

            Map<String, String> sourcePartition = new HashMap<>();
            sourcePartition.put("taigaUrl", this.taigaUrl);
            Map<String, String> sourceOffset = new HashMap<>();
            sourceOffset.put("updated", dfZULU.format(t.modified_date));
            SourceRecord sr = new SourceRecord(sourcePartition, sourceOffset, this.tasktopic.get(this.currentTaskID), Schema.STRING_SCHEMA, t.id.toString(), TaigaSchema.taigaTask, tasktemp);
            result.add(sr);
        }

        return result;
    }

    private List<SourceRecord> getTaigaUserStories(UserStory[] us, Map<Integer, Milestone> milestones, Map<Integer, String> userstoryAttributesIDs) throws InterruptedException {
        List<SourceRecord> result = new ArrayList<>();

        for (UserStory u : us) {
            if (this.count == 5) {
                Thread.sleep(10L);
                this.count = 0;
            }

            this.count = this.count + 1;
            this.log.info("US ID: " + u.id);
            this.log.info("US URL: " + this.taigaUrl + "/userstory/" + u.id);
            Struct ustemp = new Struct(TaigaSchema.taigaUserStory);
            ustemp.put("subject", u.subject);
            if (u.epics != null) {
                ustemp.put("epic_id", u.epics[0].id);
            }

            ustemp.put("id", u.id);
            ustemp.put("status", u.status_extra_info.name);
            ustemp.put("is_closed", u.is_closed);
            ustemp.put("reference", u.ref);
            if (u.assigned_to_extra_info != null) {
                ustemp.put("assigned", u.assigned_to_extra_info.username);
            }

            ustemp.put("created_date", dfZULU.format(u.created_date));
            if (u.modified_date != null) {
                ustemp.put("modified_date", dfZULU.format(u.modified_date));
            }

            if (u.finish_date != null) {
                ustemp.put("finished_date", dfZULU.format(u.finish_date));
            }

            ustemp.put("total_points", u.total_points);
            UserStory temp = TaigaApi.getUserStoryById(this.taigaUrl, u.id.toString(), this.taigaToken);
            ustemp.put("pattern", this.followsPattern(temp.description));
            Map<String, String> tempMap = TaigaApi.getCustomAttributes(u.id, "userstory", this.taigaUrl, this.taigaToken, userstoryAttributesIDs);
            if (tempMap.get("ACCEPTANCE CRITERIA") != null) {
                ustemp.put("acceptance_criteria", this.hasAcceptanceCriteria(tempMap.get("ACCEPTANCE CRITERIA")));
            } else if (tempMap.get("CRITERIS D'ACCEPTACIÓ") != null) {
                ustemp.put("acceptance_criteria", this.hasAcceptanceCriteria(tempMap.get("CRITERIS D'ACCEPTACIÓ")));
            } else if (tempMap.get("CRITERIOS DE ACEPTACIÓN") != null) {
                ustemp.put("acceptance_criteria", this.hasAcceptanceCriteria(tempMap.get("CRITERIOS DE ACEPTACIÓN")));
            } else {
                ustemp.put("acceptance_criteria", false);
            }

            if (tempMap.get("PRIORITY") != null) {
                ustemp.put("priority", tempMap.get("PRIORITY"));
            } else if (tempMap.get("PRIORITAT") != null) {
                ustemp.put("priority", tempMap.get("PRIORITAT"));
            } else if (tempMap.get("PRIORIDAD") != null) {
                ustemp.put("priority", tempMap.get("PRIORIDAD"));
            }

            if (u.milestone != null) {
                ustemp.put("milestone_id", (milestones.get(u.milestone)).id);
                ustemp.put("milestone_name", (milestones.get(u.milestone)).name);
                ustemp.put("milestone_closed_points", (milestones.get(u.milestone)).closed_points);
                ustemp.put("milestone_total_points", (milestones.get(u.milestone)).total_points);
                LocalDateTime now = LocalDateTime.now();

                try {
                    Date today = onlyDate.parse(now.toString());
                    if (!(milestones.get(u.milestone)).closed && (milestones.get(u.milestone)).estimated_start.compareTo(today) < 0) {
                        ustemp.put("milestone_closed", (milestones.get(u.milestone)).closed);
                    } else {
                        ustemp.put("milestone_closed", true);
                    }
                } catch (ParseException e) {
                    ustemp.put("milestone_closed", (milestones.get(u.milestone)).closed);
                    this.log.info("unable to parse " + now);
                    throw new InterruptedException();
                }

                ustemp.put("milestone_created_date", dfZULU.format((milestones.get(u.milestone)).created_date));
                if ((milestones.get(u.milestone)).modified_date != null) {
                    ustemp.put("milestone_modified_date", dfZULU.format((milestones.get(u.milestone)).modified_date));
                }

                if ((milestones.get(u.milestone)).estimated_start != null) {
                    ustemp.put("estimated_start", dfZULU.format((milestones.get(u.milestone)).estimated_start));
                }

                if ((milestones.get(u.milestone)).estimated_finish != null) {
                    ustemp.put("estimated_finish", dfZULU.format((milestones.get(u.milestone)).estimated_finish));
                }
            }

            Map<String, String> sourcePartition = new HashMap<>();
            sourcePartition.put("taigaUrl", this.taigaUrl);
            Map<String, String> sourceOffset = new HashMap<>();
            sourceOffset.put("updated", dfZULU.format(u.modified_date));
            SourceRecord sr = new SourceRecord(sourcePartition, sourceOffset, this.userstorytopic.get(this.currentTaskID), Schema.STRING_SCHEMA, u.id.toString(), TaigaSchema.taigaUserStory, ustemp);
            result.add(sr);
        }

        return result;
    }

    private List<SourceRecord> getTaigaEpics(Epic[] epics) throws InterruptedException {
        List<SourceRecord> result = new ArrayList<>();
        for(Epic e : epics) {
            if (this.count == 5) {
                Thread.sleep(10L);
                this.count = 0;
            }

            this.count = this.count + 1;
            this.log.info("EPIC ID: " + e.id);
            this.log.info("EPIC URL: " + this.taigaUrl + "/epics/" + e.id);
            Struct struct = new Struct(TaigaSchema.taigaEpic);
            struct.put(TaigaSchema.FIELD_TAIGA_EPIC_ID, e.id);
            struct.put(TaigaSchema.FIELD_TAIGA_EPIC_SUBJECT, e.subject);
            struct.put(TaigaSchema.FIELD_TAIGA_EPIC_STATUS, e.status_extra_info.name);
            struct.put(TaigaSchema.FIELD_TAIGA_EPIC_IS_CLOSED, e.is_closed);
            if (e.user_stories_counts != null) {
                struct.put(TaigaSchema.FIELD_TAIGA_EPIC_PROGRESS, e.user_stories_counts.progress);
                struct.put(TaigaSchema.FIELD_TAIGA_EPIC_TOTAL, e.user_stories_counts.total);
            }

            if (e.created_date != null) {
                struct.put(TaigaSchema.FIELD_TAIGA_EPIC_CREATED_DATE, dfZULU.format(e.created_date));
            }

            if (e.modified_date != null) {
                struct.put(TaigaSchema.FIELD_TAIGA_EPIC_MODIFIED_DATE, dfZULU.format(e.modified_date));
            }

            Map<String, String> sourcePartition = new HashMap<>();
            sourcePartition.put("taigaUrl", this.taigaUrl);
            Map<String, String> sourceOffset = new HashMap<>();
            sourceOffset.put("updated", dfZULU.format(e.modified_date));
            SourceRecord sr = new SourceRecord(sourcePartition, sourceOffset, this.epictopic.get(this.currentTaskID), Schema.STRING_SCHEMA, e.id.toString(), TaigaSchema.taigaEpic, struct);
            result.add(sr);
        }

        return result;
    }

    private List<SourceRecord> getIssueSourceRecords(Issue[] redmineIssues, Project myProject) throws InterruptedException {
        List<SourceRecord> result = new ArrayList<>();

        for(Issue i : redmineIssues) {
            if (this.count == 5) {
                Thread.sleep(10L);
                this.count = 0;
            }

            this.count = this.count + 1;
            this.log.info("ISSUE ID: " + i.id);
            this.log.info("ISSUE URL: " + this.taigaUrl + "/issues/" + i.id);
            Struct struct = new Struct(TaigaSchema.taigaIssue);
            struct.put(TaigaSchema.FIELD_TAIGA_ISSUE_SUBJECT, i.subject);
            struct.put(TaigaSchema.FIELD_TAIGA_ISSUE_DESCRIPTION, i.description);
            struct.put(TaigaSchema.FIELD_TAIGA_ISSUE_ID, i.id);
            struct.put(TaigaSchema.FIELD_TAIGA_ISSUE_STATUS, i.status_extra_info.name);
            struct.put(TaigaSchema.FIELD_TAIGA_ISSUE_IS_CLOSED, i.is_closed);
            if (i.created_date != null) {
                struct.put(TaigaSchema.FIELD_TAIGA_ISSUE_CREATED_DATE, dfZULU.format(i.created_date));
            }

            if (i.modified_date != null) {
                struct.put(TaigaSchema.FIELD_TAIGA_ISSUE_MODIFIED_DATE, dfZULU.format(i.modified_date));
            }

            if (i.finished_date != null) {
                struct.put(TaigaSchema.FIELD_TAIGA_ISSUE_FINISHED_DATE, dfZULU.format(i.finished_date));
            }

            if (i.assigned_to_extra_info != null) {
                struct.put(TaigaSchema.FIELD_TAIGA_ISSUE_ASSIGNED, i.assigned_to_extra_info.username);
            }

            int x = 0;

            boolean found;
            for(found = false; !found && x < myProject.severities.length; ++x) {
                if (myProject.severities[x].id.equals(i.severity)) {
                    found = true;
                    struct.put(TaigaSchema.FIELD_TAIGA_ISSUE_SEVERITY, myProject.severities[x].name);
                }
            }

            x = 0;

            for(found = false; !found && x < myProject.priorities.length; ++x) {
                if (myProject.priorities[x].id.equals(i.priority)) {
                    found = true;
                    struct.put(TaigaSchema.FIELD_TAIGA_ISSUE_PRIORITY, myProject.priorities[x].name);
                }
            }

            x = 0;

            for(found = false; !found && x < myProject.issue_types.length; ++x) {
                if (myProject.issue_types[x].id.equals(i.type)) {
                    found = true;
                    struct.put(TaigaSchema.FIELD_TAIGA_ISSUE_TYPE, myProject.issue_types[x].name);
                }
            }

            Map<String, String> sourcePartition = new HashMap<>();
            sourcePartition.put("taigaUrl", this.taigaUrl);
            Map<String, String> sourceOffset = new HashMap<>();
            sourceOffset.put("updated", dfZULU.format(i.modified_date));
            SourceRecord sr = new SourceRecord(sourcePartition, sourceOffset, this.issuetopic.get(this.currentTaskID), Schema.STRING_SCHEMA, i.id.toString(), TaigaSchema.taigaIssue, struct);
            result.add(sr);
        }

        this.log.info("Found " + result.size() + " issues.");
        return result;
    }

    public void start(Map<String, String> props) {
        this.log.info("connect-taiga: start task");
        this.log.info(props.toString());

        this.taigaUrl = props.get("taiga.url");
        this.taigaUser = props.get("username");
        this.taigaPass = props.get("password");
        this.taigaInterval = props.get("taiga.interval.seconds");
        this.taigaTeamsNum = props.get("taiga.teams.num");
        this.taigaTeamsInterval = props.get("taiga.teams.interval.seconds");
        this.taigaTaskCustomAttributes = props.get("taskCustomAttributes");
        this.taigaUserstoryCustomAttributes = props.get("userstoryCustomAttributes");

        this.taigaSlug = new ArrayList<>();
        this.issuetopic = new ArrayList<>();
        this.epictopic = new ArrayList<>();
        this.userstorytopic = new ArrayList<>();
        this.tasktopic = new ArrayList<>();

        this.teamsNum = Integer.parseInt(this.taigaTeamsNum);
        for (int i = 0; i < teamsNum; ++i) {
            this.taigaSlug.add( props.get("tasks." + i + ".slug") );
            this.issuetopic.add( props.get("tasks." + i + ".taiga.issue.topic") );
            this.epictopic.add( props.get("tasks." + i + ".taiga.epic.topic") );
            this.userstorytopic.add( props.get("tasks." + i + ".taiga.userstory.topic") );
            this.tasktopic.add( props.get("tasks." + i + ".taiga.task.topic") );
        }

        this.log.info("taiga.url: " + this.taigaUrl);
        this.log.info("taiga.interval.seconds: " + taigaInterval);

        if (taigaInterval == null || taigaInterval.isEmpty()) this.interval = 3600;
        else this.interval = Integer.parseInt(taigaInterval);
        if (taigaTeamsInterval == null || taigaTeamsInterval.isEmpty()) this.teamsInterval = 120;
        else this.teamsInterval = Integer.parseInt(taigaTeamsInterval);
        currentTaskID = 0;
    }

    public void stop() {
    }

    public String version() {
        return "0.0.1";
    }

    static {
        dfZULU.setTimeZone(tzUTC);
        onlyDate = new SimpleDateFormat("yyyy-MM-dd");
    }
}