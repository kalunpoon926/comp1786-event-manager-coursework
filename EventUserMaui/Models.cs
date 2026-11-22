namespace EventUserMaui;

public sealed class EventResponse
{
    public List<EventItem> Events { get; set; } = [];
}

public sealed class EventItem
{
    public string Id { get; set; } = "";
    public string Name { get; set; } = "";
    public string Date { get; set; } = "";
    public string Location { get; set; } = "";
    public string Organiser { get; set; } = "";
    public string Type { get; set; } = "";
    public string Status { get; set; } = "";
    public string Description { get; set; } = "";
    public List<SessionItem> Sessions { get; set; } = [];
}

public sealed class SessionItem
{
    public string Id { get; set; } = "";
    public string Name { get; set; } = "";
    public string Time { get; set; } = "";
    public string Speaker { get; set; } = "";
    public string Room { get; set; } = "";
    public string Type { get; set; } = "";
    public string Description { get; set; } = "";
}
