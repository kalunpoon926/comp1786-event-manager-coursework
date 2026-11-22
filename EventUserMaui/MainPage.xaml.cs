using System.Globalization;
using System.Text.Json;
using Microsoft.Maui.Storage;
using Microsoft.Maui.Controls.Shapes;

namespace EventUserMaui;

public partial class MainPage : ContentPage
{
    private readonly EventApiClient api = new();
    private List<EventItem> events = [];
    private List<EventItem> cachedEvents = [];
    private HashSet<string> favourites = [];
    private bool showingFavourites;
    private bool loaded;
    private string baseUrl = EventApiClient.DefaultBaseUrl;
    private string CachePath => System.IO.Path.Combine(FileSystem.Current.AppDataDirectory, "cached-events.json");

    public MainPage() => InitializeComponent();

    protected override async void OnAppearing()
    {
        base.OnAppearing();
        if (loaded) return;
        loaded = true;
        baseUrl = Preferences.Default.Get("cloud_url", EventApiClient.DefaultBaseUrl);
        ApiUrlEntry.Text = baseUrl;
        try
        {
            favourites = JsonSerializer.Deserialize<HashSet<string>>(Preferences.Default.Get("favourite_ids", "[]")) ?? [];
            if (File.Exists(CachePath)) cachedEvents = JsonSerializer.Deserialize<List<EventItem>>(await File.ReadAllTextAsync(CachePath),
                new JsonSerializerOptions { PropertyNameCaseInsensitive = true }) ?? [];
            events = cachedEvents;
        }
        catch { StatusLabel.Text = "Saved events could not be read."; }
        Render();
        await RefreshAsync();
    }

    private async Task RefreshAsync()
    {
        var name = NameEntry.Text?.Trim() ?? "";
        var date = DateEntry.Text?.Trim() ?? "";
        if (date.Length > 0 && !DateOnly.TryParseExact(date, "dd/MM/yyyy", CultureInfo.InvariantCulture, DateTimeStyles.None, out _))
        {
            StatusLabel.Text = "Enter a valid date as DD/MM/YYYY.";
            return;
        }
        StatusLabel.Text = "Loading events…";
        try
        {
            events = await api.SearchAsync(baseUrl, name, date);
            if (name.Length == 0 && date.Length == 0)
            {
                cachedEvents = events;
                await File.WriteAllTextAsync(CachePath, JsonSerializer.Serialize(events));
            }
            StatusLabel.Text = $"{events.Count} upcoming event{(events.Count == 1 ? "" : "s")} found.";
        }
        catch (Exception ex)
        {
            events = cachedEvents;
            StatusLabel.Text = $"Cloud unavailable ({ex.Message}). Showing saved events if available.";
        }
        Render();
    }

    private void Render()
    {
        UpcomingButton.BackgroundColor = Color.FromArgb(showingFavourites ? "#DCE7F5" : "#26375D");
        UpcomingButton.TextColor = Color.FromArgb(showingFavourites ? "#26375D" : "#FFFFFF");
        FavouritesButton.BackgroundColor = Color.FromArgb(showingFavourites ? "#26375D" : "#DCE7F5");
        FavouritesButton.TextColor = Color.FromArgb(showingFavourites ? "#FFFFFF" : "#26375D");
        FavouritesButton.Text = $"Favourites ({favourites.Count})";
        EventList.Children.Clear();
        var shown = (showingFavourites ? cachedEvents : events).Where(e => !showingFavourites || favourites.Contains(e.Id)).ToList();
        if (shown.Count == 0)
        {
            EventList.Children.Add(new Label { Text = showingFavourites ? "No favourites yet. Tap ☆ on an event." : "No upcoming events found.", TextColor = Color.FromArgb("#566477") });
            return;
        }
        foreach (var e in shown)
        {
            var text = new VerticalStackLayout { Spacing = 4, VerticalOptions = LayoutOptions.Center };
            text.Children.Add(new Label { Text = e.Name, FontSize = 17, FontAttributes = FontAttributes.Bold, TextColor = Color.FromArgb("#1C2740") });
            text.Children.Add(new Label { Text = $"{e.Date} · {e.Location}", TextColor = Color.FromArgb("#566477") });
            text.Children.Add(new Label { Text = $"{e.Type} · {e.Sessions.Count} sessions", TextColor = Color.FromArgb("#566477") });
            var detail = new Button { Text = "Details", BackgroundColor = Color.FromArgb("#27569A"), FontSize = 13 };
            detail.Clicked += async (_, _) => await Navigation.PushAsync(new EventDetailPage(e));
            var star = new Button { Text = favourites.Contains(e.Id) ? "★" : "☆", BackgroundColor = Color.FromArgb("#E9EFF8"), TextColor = Color.FromArgb("#26375D"), FontSize = 20 };
            star.Clicked += (_, _) => { if (!favourites.Add(e.Id)) favourites.Remove(e.Id); Preferences.Default.Set("favourite_ids", JsonSerializer.Serialize(favourites)); Render(); };
            var buttons = new VerticalStackLayout { Spacing = 5, WidthRequest = 94 };
            buttons.Children.Add(detail); buttons.Children.Add(star);
            var row = new Grid { ColumnDefinitions = { new ColumnDefinition(GridLength.Star), new ColumnDefinition(GridLength.Auto) }, ColumnSpacing = 8 };
            row.Add(text, 0, 0); row.Add(buttons, 1, 0);
            EventList.Children.Add(new Border { BackgroundColor = Colors.White, StrokeThickness = 0, StrokeShape = new RoundRectangle { CornerRadius = new CornerRadius(14) }, Padding = 15, Content = row });
        }
    }

    private async void OnSearchClicked(object sender, EventArgs e) { showingFavourites = false; await RefreshAsync(); }
    private async void OnClearClicked(object sender, EventArgs e) { NameEntry.Text = ""; DateEntry.Text = ""; showingFavourites = false; await RefreshAsync(); }
    private void OnUpcomingClicked(object sender, EventArgs e) { showingFavourites = false; Render(); }
    private void OnFavouritesClicked(object sender, EventArgs e) { showingFavourites = true; Render(); }
    private async void OnSaveServerClicked(object sender, EventArgs e)
    {
        var value = ApiUrlEntry.Text?.Trim().TrimEnd('/') ?? "";
        if (!Uri.TryCreate(value, UriKind.Absolute, out var uri) || uri.Scheme != Uri.UriSchemeHttps)
        { StatusLabel.Text = "Enter a full HTTPS API URL."; return; }
        baseUrl = value;
        Preferences.Default.Set("cloud_url", value);
        await RefreshAsync();
    }
}
