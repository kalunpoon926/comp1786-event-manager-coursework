using System.Net.Http.Json;
using System.Text.Json;

namespace EventUserMaui;

public sealed class EventApiClient
{
    public const string DefaultBaseUrl = "https://comp1786-event-cloud-api.comp1786-event-cloud.workers.dev";
    private static readonly HttpClient Client = new() { Timeout = TimeSpan.FromSeconds(15) };
    private static readonly JsonSerializerOptions JsonOptions = new() { PropertyNameCaseInsensitive = true };

    public async Task<List<EventItem>> SearchAsync(string baseUrl, string name, string date)
    {
        var url = baseUrl.TrimEnd('/') + "/api/events?name=" + Uri.EscapeDataString(name)
                  + "&date=" + Uri.EscapeDataString(date);
        var response = await Client.GetAsync(url);
        response.EnsureSuccessStatusCode();
        var body = await response.Content.ReadFromJsonAsync<EventResponse>(JsonOptions);
        return body?.Events ?? throw new InvalidDataException("The cloud response is empty.");
    }
}
