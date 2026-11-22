using Microsoft.Maui.Controls.Shapes;

namespace EventUserMaui;

public sealed class EventDetailPage : ContentPage
{
    public EventDetailPage(EventItem e)
    {
        Title = e.Name;
        BackgroundColor = Color.FromArgb("#F3F6FB");
        var stack = new VerticalStackLayout { Padding = 18, Spacing = 10 };
        stack.Children.Add(new Label { Text = e.Name, FontSize = 25, FontAttributes = FontAttributes.Bold, TextColor = Color.FromArgb("#26375D") });
        foreach (var line in new[] { $"Date: {e.Date}", $"Location: {e.Location}", $"Organiser: {e.Organiser}", $"Type: {e.Type}", $"Status: {e.Status}", $"Description: {(string.IsNullOrWhiteSpace(e.Description) ? "—" : e.Description)}" })
            stack.Children.Add(new Label { Text = line, FontSize = 15 });
        stack.Children.Add(new Label { Text = $"Sessions ({e.Sessions.Count})", FontSize = 20, FontAttributes = FontAttributes.Bold, Margin = new Thickness(0, 12, 0, 0) });
        foreach (var session in e.Sessions)
        {
            var details = new VerticalStackLayout { Spacing = 4 };
            details.Children.Add(new Label { Text = session.Name, FontSize = 17, FontAttributes = FontAttributes.Bold });
            details.Children.Add(new Label { Text = $"{session.Time} · {session.Type}" });
            details.Children.Add(new Label { Text = $"Speaker: {(string.IsNullOrEmpty(session.Speaker) ? "—" : session.Speaker)} · Room: {(string.IsNullOrEmpty(session.Room) ? "—" : session.Room)}" });
            if (!string.IsNullOrWhiteSpace(session.Description)) details.Children.Add(new Label { Text = session.Description });
            stack.Children.Add(new Border { BackgroundColor = Colors.White, StrokeThickness = 0, StrokeShape = new RoundRectangle { CornerRadius = new CornerRadius(12) }, Padding = 14, Content = details });
        }
        Content = new ScrollView { Content = stack };
    }
}
