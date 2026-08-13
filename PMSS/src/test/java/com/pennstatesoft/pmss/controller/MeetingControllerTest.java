package com.pennstatesoft.pmss.controller;

import com.pennstatesoft.pmss.model.Client;
import com.pennstatesoft.pmss.model.Meeting;
import com.pennstatesoft.pmss.model.User;
import com.pennstatesoft.pmss.security.SecurityLogger;
import com.pennstatesoft.pmss.service.MeetingService;
import com.pennstatesoft.pmss.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.Model;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;

import java.sql.Date;
import java.sql.Time;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MeetingControllerTest {

    private final MeetingService meetingService = mock(MeetingService.class);
    private final UserService userService = mock(UserService.class);
    private final JdbcTemplate jdbc = mock(JdbcTemplate.class);
    private final SecurityLogger securityLogger = mock(SecurityLogger.class);
    private final MeetingController controller =
            new MeetingController(meetingService, userService, jdbc, securityLogger);

    private static final int USER_ID = 3;

    private User user() {
        return new Client(USER_ID, "u@pennstatesoft.com", "h", "L", "F", "CLIENT",
                "2000-01-01", "F L", null, 0, null, null);
    }

    private Authentication auth() {
        Authentication a = mock(Authentication.class);
        when(a.getName()).thenReturn("u@pennstatesoft.com");
        when(userService.findByEmail("u@pennstatesoft.com")).thenReturn(user());
        return a;
    }

    private Meeting meetingOwnedBy(int creatorID) {
        Meeting m = new Meeting(11, "Existing", Date.valueOf("2026-05-20"), 5);
        m.setStartTime(Time.valueOf("09:00:00"));
        m.setEndTime(Time.valueOf("10:00:00"));
        m.setCreatorID(creatorID);
        m.setStatus("SCHEDULED");
        return m;
    }

    private void stubRoomAvailable(int conflicts) {
        when(jdbc.queryForObject(anyString(), eq(Integer.class), any(), any(), any(), any(), any()))
                .thenReturn(conflicts);
    }

    private void stubClientAvailable(int conflicts) {
        when(jdbc.queryForObject(anyString(), eq(Integer.class), any(), any(), any(), any(), any(), any()))
                .thenReturn(conflicts);
    }

    private void stubFindRoom(String roomType, double fee) {
        when(jdbc.queryForList(anyString(), any(Object[].class)))
                .thenReturn(List.<Map<String, Object>>of(Map.of("roomNumber", 5, "roomType", roomType, "fee", fee)));
    }

    private String error(Model model) {
        return (String) model.getAttribute("errorMessage");
    }

    // ---- simple / create form ----

    @Test
    void displayMeetingsRedirectsToSchedule() {
        assertEquals("redirect:/schedule", controller.displayMeetings());
    }

    @Test
    void showCreateMeetingPageReturnsForm() {
        when(jdbc.queryForList(anyString())).thenReturn(List.of());
        Model model = new ExtendedModelMap();

        assertEquals("create-meeting", controller.showCreateMeetingPage(model, auth()));
    }

    // ---- createMeeting ----

    @Test
    void createMeetingRejectsBlankName() {
        when(jdbc.queryForList(anyString())).thenReturn(List.of());
        Model model = new ExtendedModelMap();

        String view = controller.createMeeting("", "2026-05-20", "09:00", "10:00", 5, false,
                model, auth(), new RedirectAttributesModelMap());

        assertEquals("create-meeting", view);
        assertEquals("Meeting name is required.", error(model));
    }

    @Test
    void createMeetingRejectsOutsideBusinessHours() {
        when(jdbc.queryForList(anyString())).thenReturn(List.of());
        Model model = new ExtendedModelMap();

        String view = controller.createMeeting("Standup", "2026-05-20", "08:00", "10:00", 5, false,
                model, auth(), new RedirectAttributesModelMap());

        assertEquals("create-meeting", view);
        assertEquals("Meetings must be within business hours (09:00–17:00).", error(model));
    }

    @Test
    void createMeetingRejectsWhenRoomBooked() {
        stubRoomAvailable(1);
        when(jdbc.queryForList(anyString())).thenReturn(List.of());
        Model model = new ExtendedModelMap();

        String view = controller.createMeeting("Standup", "2026-05-20", "09:00", "10:00", 5, false,
                model, auth(), new RedirectAttributesModelMap());

        assertEquals("create-meeting", view);
        assertEquals("Room 5 is already booked during that time slot.", error(model));
    }

    @Test
    void createMeetingRejectsWhenCreatorDoubleBooked() {
        stubRoomAvailable(0);
        stubClientAvailable(1);
        when(jdbc.queryForList(anyString())).thenReturn(List.of());
        Model model = new ExtendedModelMap();

        String view = controller.createMeeting("Standup", "2026-05-20", "09:00", "10:00", 5, false,
                model, auth(), new RedirectAttributesModelMap());

        assertEquals("create-meeting", view);
        assertEquals("You already have a meeting during that time slot.", error(model));
    }

    @Test
    void createMeetingShowsPaymentConfirmationForSpecialRoom() {
        stubRoomAvailable(0);
        stubClientAvailable(0);
        stubFindRoom("SPECIAL", 100.0);
        Model model = new ExtendedModelMap();

        String view = controller.createMeeting("Board Review", "2026-05-20", "09:00", "10:00", 5, false,
                model, auth(), new RedirectAttributesModelMap());

        assertEquals("confirm-payment", view);
        assertEquals(100.0, model.getAttribute("fee"));
        verify(meetingService, never()).createMeeting(any());
    }

    @Test
    void createMeetingCreatesRegularMeeting() {
        stubRoomAvailable(0);
        stubClientAvailable(0);
        stubFindRoom("REGULAR", 0.0);
        RedirectAttributesModelMap ra = new RedirectAttributesModelMap();

        String view = controller.createMeeting("Standup", "2026-05-20", "09:00", "10:00", 5, false,
                new ExtendedModelMap(), auth(), ra);

        assertEquals("redirect:/schedule", view);
        assertEquals("Meeting \"Standup\" created.", ra.getFlashAttributes().get("successMessage"));
        verify(meetingService).createMeeting(any(Meeting.class));
        verify(securityLogger).meetingCreated("u@pennstatesoft.com", "Standup");
    }

    @Test
    void createMeetingCreatesSpecialRoomWhenConfirmed() {
        stubRoomAvailable(0);
        stubClientAvailable(0);
        stubFindRoom("SPECIAL", 100.0);
        RedirectAttributesModelMap ra = new RedirectAttributesModelMap();

        String view = controller.createMeeting("Board Review", "2026-05-20", "09:00", "10:00", 5, true,
                new ExtendedModelMap(), auth(), ra);

        assertEquals("redirect:/schedule", view);
        assertEquals("Meeting \"Board Review\" reserved — $100.00 paid for Room 5.",
                ra.getFlashAttributes().get("successMessage"));
        verify(meetingService).createMeeting(any(Meeting.class));
    }

    // ---- edit / creator guard ----

    @Test
    void showEditRedirectsWhenMeetingMissing() {
        when(meetingService.getMeeting(11)).thenReturn(null);
        RedirectAttributesModelMap ra = new RedirectAttributesModelMap();

        String view = controller.showEditMeetingPage(11, new ExtendedModelMap(), auth(), ra);

        assertEquals("redirect:/schedule", view);
        assertEquals("Meeting not found.", ra.getFlashAttributes().get("errorMessage"));
    }

    @Test
    void showEditRedirectsWhenNotCreator() {
        when(meetingService.getMeeting(11)).thenReturn(meetingOwnedBy(999));
        RedirectAttributesModelMap ra = new RedirectAttributesModelMap();

        String view = controller.showEditMeetingPage(11, new ExtendedModelMap(), auth(), ra);

        assertEquals("redirect:/schedule", view);
        assertEquals("Only the meeting's creator can manage it.",
                ra.getFlashAttributes().get("errorMessage"));
    }

    @Test
    void showEditRendersFormForCreator() {
        when(meetingService.getMeeting(11)).thenReturn(meetingOwnedBy(USER_ID));
        when(jdbc.queryForList(anyString())).thenReturn(List.of());
        when(jdbc.queryForList(anyString(), any(Object[].class))).thenReturn(List.of());
        Model model = new ExtendedModelMap();

        String view = controller.showEditMeetingPage(11, model, auth(), new RedirectAttributesModelMap());

        assertEquals("edit-meeting", view);
        assertEquals(11, model.getAttribute("meetingId"));
    }

    // ---- delete ----

    @Test
    void deleteRemovesMeetingForCreator() {
        when(meetingService.getMeeting(11)).thenReturn(meetingOwnedBy(USER_ID));
        RedirectAttributesModelMap ra = new RedirectAttributesModelMap();

        String view = controller.deleteMeeting(11, auth(), ra);

        assertEquals("redirect:/schedule", view);
        verify(jdbc).update("DELETE FROM MeetingAttendees WHERE meetingID = ?", 11);
        verify(meetingService).deleteMeeting(11);
        verify(securityLogger).meetingDeleted("u@pennstatesoft.com", 11);
    }

    @Test
    void deleteRejectedForNonCreator() {
        when(meetingService.getMeeting(11)).thenReturn(meetingOwnedBy(999));
        RedirectAttributesModelMap ra = new RedirectAttributesModelMap();

        String view = controller.deleteMeeting(11, auth(), ra);

        assertEquals("redirect:/schedule", view);
        verify(meetingService, never()).deleteMeeting(anyInt());
    }

    // ---- attendees ----

    @Test
    void addParticipantRequiresSelection() {
        when(meetingService.getMeeting(11)).thenReturn(meetingOwnedBy(USER_ID));
        RedirectAttributesModelMap ra = new RedirectAttributesModelMap();

        String view = controller.addParticipant(11, null, auth(), ra);

        assertEquals("redirect:/meetings/11/edit", view);
        assertEquals("Please choose a person to add.", ra.getFlashAttributes().get("errorMessage"));
    }

    @Test
    void addParticipantRejectsBusyPerson() {
        when(meetingService.getMeeting(11)).thenReturn(meetingOwnedBy(USER_ID));
        stubClientAvailable(1);
        RedirectAttributesModelMap ra = new RedirectAttributesModelMap();

        String view = controller.addParticipant(11, 8, auth(), ra);

        assertEquals("redirect:/meetings/11/edit", view);
        assertEquals("That person already has a meeting during this time slot.",
                ra.getFlashAttributes().get("errorMessage"));
        verify(jdbc, never()).update(anyString(), eq(11), eq(8));
    }

    @Test
    void addParticipantInsertsWhenFree() {
        when(meetingService.getMeeting(11)).thenReturn(meetingOwnedBy(USER_ID));
        stubClientAvailable(0);
        RedirectAttributesModelMap ra = new RedirectAttributesModelMap();

        String view = controller.addParticipant(11, 8, auth(), ra);

        assertEquals("redirect:/meetings/11/edit", view);
        verify(jdbc).update("INSERT OR IGNORE INTO MeetingAttendees (meetingID, userID) VALUES (?, ?)", 11, 8);
        verify(securityLogger).attendeeAdded("u@pennstatesoft.com", 11, 8);
    }

    @Test
    void removeParticipantDeletesAttendee() {
        when(meetingService.getMeeting(11)).thenReturn(meetingOwnedBy(USER_ID));
        RedirectAttributesModelMap ra = new RedirectAttributesModelMap();

        String view = controller.removeParticipant(11, 8, auth(), ra);

        assertEquals("redirect:/meetings/11/edit", view);
        verify(jdbc).update("DELETE FROM MeetingAttendees WHERE meetingID = ? AND userID = ?", 11, 8);
        verify(securityLogger).attendeeRemoved("u@pennstatesoft.com", 11, 8);
    }
}
