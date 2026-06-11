package com.example.planify.data.remote;

import com.example.planify.data.remote.model.CalendarEvent;

import java.util.List;

// Placeholder - CalendarApiService is handled via Google Calendar Java Client Library
// This interface is kept for potential REST-style calls
public interface CalendarApiService {
    interface CalendarCallback {
        void onSuccess(List<CalendarEvent> events);
        void onError(Exception e);
    }

    interface CreateEventCallback {
        void onSuccess(String eventId);
        void onError(Exception e);
    }

    interface DeleteCallback {
        void onSuccess();
        void onError(Exception e);
    }

    interface UpdateEventCallback {
        void onSuccess();
        void onError(Exception e);
    }
}
